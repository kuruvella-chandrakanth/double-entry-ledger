package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.CreateTransactionRequest;
import com.nuplay.ledger.dto.CreateTransactionRequest.EntryLine;
import com.nuplay.ledger.dto.TransactionResponse;
import com.nuplay.ledger.exception.AccountNotFoundException;
import com.nuplay.ledger.exception.ImbalancedTransactionException;
import com.nuplay.ledger.model.Account;
import com.nuplay.ledger.model.LedgerEntry;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.model.LedgerTransaction;
import com.nuplay.ledger.repository.AccountRepository;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import com.nuplay.ledger.repository.LedgerTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final AccountRepository accountRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse postTransaction(CreateTransactionRequest request) {
        // Idempotency: same key already committed → return original, don't post again
        return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
            .map(this::toResponse)
            .orElseGet(() -> createNewTransaction(request));
    }

    private TransactionResponse createNewTransaction(CreateTransactionRequest request) {
        // 1. Validate balance before any DB write
        validateBalance(request.getEntries());

        // 2. Lock accounts in ID order — prevents deadlocks under concurrency
        List<Long> accountIds = request.getEntries().stream()
            .map(EntryLine::getAccountId)
            .distinct()
            .sorted()
            .toList();

        List<Account> accounts = accountRepository.findAllByIdForUpdate(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new AccountNotFoundException(accountIds.getFirst());
        }

        Map<Long, Account> accountMap = accounts.stream()
            .collect(Collectors.toMap(Account::getId, Function.identity()));

        // 3. Save transaction header
        LedgerTransaction tx = new LedgerTransaction();
        tx.setIdempotencyKey(request.getIdempotencyKey());
        tx.setDescription(request.getDescription());
        LedgerTransaction savedTx = transactionRepository.save(tx);

        // 4. Save individual ledger entries
        List<LedgerEntry> entries = request.getEntries().stream().map(line -> {
            LedgerEntry entry = new LedgerEntry();
            entry.setTransaction(savedTx);
            entry.setAccount(accountMap.get(line.getAccountId()));
            entry.setEntryType(line.getEntryType());
            entry.setAmount(line.getAmount());
            return entry;
        }).toList();

        savedTx.setEntries(entryRepository.saveAll(entries));
        return toResponse(savedTx);
    }

    // Reject before touching DB if sum(DEBIT) != sum(CREDIT)
    private void validateBalance(List<EntryLine> entries) {
        long debits  = entries.stream().filter(e -> e.getEntryType() == EntryType.DEBIT).mapToLong(EntryLine::getAmount).sum();
        long credits = entries.stream().filter(e -> e.getEntryType() == EntryType.CREDIT).mapToLong(EntryLine::getAmount).sum();
        if (debits != credits) {
            throw new ImbalancedTransactionException(debits, credits);
        }
    }

    public TransactionResponse getTransaction(Long id) {
        LedgerTransaction tx = transactionRepository.findById(id)
            .orElseThrow(() -> new com.nuplay.ledger.exception.TransactionNotFoundException(id));
        return toResponse(tx);
    }

    private TransactionResponse toResponse(LedgerTransaction tx) {
        List<TransactionResponse.EntryLineResponse> entryResponses = tx.getEntries().stream()
            .map(e -> TransactionResponse.EntryLineResponse.builder()
                .accountId(e.getAccount().getId())
                .accountName(e.getAccount().getName())
                .entryType(e.getEntryType())
                .amount(e.getAmount())
                .build())
            .toList();

        return TransactionResponse.builder()
            .id(tx.getId())
            .idempotencyKey(tx.getIdempotencyKey())
            .description(tx.getDescription())
            .createdAt(tx.getCreatedAt())
            .entries(entryResponses)
            .build();
    }
}
