package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.CreateTransactionRequest;
import com.nuplay.ledger.dto.CreateTransactionRequest.EntryLine;
import com.nuplay.ledger.dto.TransactionResponse;
import com.nuplay.ledger.exception.ImbalancedTransactionException;
import com.nuplay.ledger.model.Account;
import com.nuplay.ledger.model.LedgerEntry;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.model.LedgerTransaction;
import com.nuplay.ledger.repository.AccountRepository;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import com.nuplay.ledger.repository.LedgerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private LedgerTransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository entryRepository;
    @Mock private AccountRepository accountRepository;
    @InjectMocks private TransactionService transactionService;

    private Account wallet;
    private Account cashPool;

    @BeforeEach
    void setUp() {
        wallet   = account(1L, "User Wallet");
        cashPool = account(2L, "Cash Pool");
    }

    @Test
    @DisplayName("Posts a balanced transaction successfully")
    void postsBalancedTransaction() {
        CreateTransactionRequest request = request("key-1", List.of(
            entry(1L, EntryType.DEBIT,  5000L),
            entry(2L, EntryType.CREDIT, 5000L)
        ));

        when(transactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountRepository.findAllByIdForUpdate(anyList())).thenReturn(List.of(wallet, cashPool));

        LedgerTransaction savedTx = savedTx("key-1");
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(entryRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<LedgerEntry> entries = inv.getArgument(0);
            savedTx.setEntries(entries);
            return entries;
        });

        TransactionResponse response = transactionService.postTransaction(request);

        assertThat(response.getEntries()).hasSize(2);
    }

    @Test
    @DisplayName("Rejects an imbalanced transaction before any DB write")
    void rejectsImbalancedTransaction() {
        CreateTransactionRequest request = request("key-2", List.of(
            entry(1L, EntryType.DEBIT,  9000L),
            entry(2L, EntryType.CREDIT, 5000L)
        ));
        when(transactionRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.postTransaction(request))
            .isInstanceOf(ImbalancedTransactionException.class);

        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Returns original transaction on duplicate idempotency key — no second write")
    void idempotentOnDuplicateKey() {
        LedgerTransaction existing = savedTx("dup-key");
        existing.setId(99L);
        existing.setEntries(List.of(
            entry(existing, wallet,   EntryType.DEBIT,  5000L),
            entry(existing, cashPool, EntryType.CREDIT, 5000L)
        ));
        when(transactionRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        TransactionResponse response = transactionService.postTransaction(
            request("dup-key", List.of(
                entry(1L, EntryType.DEBIT,  5000L),
                entry(2L, EntryType.CREDIT, 5000L)
            ))
        );

        assertThat(response.getId()).isEqualTo(99L);
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(entryRepository);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Account account(Long id, String name) {
        Account a = new Account();
        a.setId(id);
        a.setName(name);
        return a;
    }

    private LedgerTransaction savedTx(String key) {
        LedgerTransaction tx = new LedgerTransaction();
        tx.setId(1L);
        tx.setIdempotencyKey(key);
        tx.setEntries(List.of());
        return tx;
    }

    private CreateTransactionRequest request(String key, List<EntryLine> lines) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setIdempotencyKey(key);
        r.setDescription("Test");
        r.setEntries(lines);
        return r;
    }

    private EntryLine entry(Long accountId, EntryType type, Long amount) {
        EntryLine l = new EntryLine();
        l.setAccountId(accountId);
        l.setEntryType(type);
        l.setAmount(amount);
        return l;
    }

    private LedgerEntry entry(LedgerTransaction tx, Account account, EntryType type, Long amount) {
        LedgerEntry e = new LedgerEntry();
        e.setTransaction(tx);
        e.setAccount(account);
        e.setEntryType(type);
        e.setAmount(amount);
        return e;
    }
}
