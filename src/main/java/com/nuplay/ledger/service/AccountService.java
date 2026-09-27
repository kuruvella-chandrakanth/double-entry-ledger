package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.AccountBalanceResponse;
import com.nuplay.ledger.dto.CreateAccountRequest;
import com.nuplay.ledger.dto.StatementResponse;
import com.nuplay.ledger.dto.StatementResponse.StatementEntry;
import com.nuplay.ledger.exception.AccountNotFoundException;
import com.nuplay.ledger.model.Account;
import com.nuplay.ledger.model.LedgerEntry;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.repository.AccountRepository;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setName(request.getName());
        return accountRepository.save(account);
    }

    /**
     * Balance = sum(CREDIT) - sum(DEBIT).
     * Credits add money (top-up, refund). Debits remove it (purchase, withdrawal).
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        long credits = entryRepository.sumByAccountIdAndEntryType(accountId, EntryType.CREDIT);
        long debits  = entryRepository.sumByAccountIdAndEntryType(accountId, EntryType.DEBIT);

        return AccountBalanceResponse.builder()
            .accountId(account.getId())
            .name(account.getName())
            .balance(credits - debits)
            .build();
    }

    /**
     * Returns all ledger entries for an account in chronological order,
     * each with a running balance — like a bank statement.
     * Entries are immutable: history never changes.
     */
    @Transactional(readOnly = true)
    public StatementResponse getStatement(Long accountId, int page, int size) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        Page<LedgerEntry> entryPage = entryRepository.findByAccountId(
            accountId, PageRequest.of(page, size)
        );

        List<StatementEntry> statementEntries = new ArrayList<>();
        long runningBalance = 0L;

        for (LedgerEntry entry : entryPage.getContent()) {
            runningBalance += entry.getEntryType() == EntryType.CREDIT
                ? entry.getAmount()
                : -entry.getAmount();

            statementEntries.add(StatementEntry.builder()
                .entryId(entry.getId())
                .transactionId(entry.getTransaction().getId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .runningBalance(runningBalance)
                .description(entry.getTransaction().getDescription())
                .createdAt(entry.getCreatedAt())
                .build());
        }

        return StatementResponse.builder()
            .accountId(account.getId())
            .accountName(account.getName())
            .entries(statementEntries)
            .build();
    }
}
