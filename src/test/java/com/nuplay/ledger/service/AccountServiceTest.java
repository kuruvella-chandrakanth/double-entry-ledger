package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.AccountBalanceResponse;
import com.nuplay.ledger.exception.AccountNotFoundException;
import com.nuplay.ledger.model.Account;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.repository.AccountRepository;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private LedgerEntryRepository entryRepository;
    @InjectMocks private AccountService accountService;

    @Test
    @DisplayName("Balance = credits - debits")
    void balanceIsCreditsMinusDebits() {
        Account account = account(1L, "User Wallet");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(entryRepository.sumByAccountIdAndEntryType(1L, EntryType.CREDIT)).thenReturn(10000L);
        when(entryRepository.sumByAccountIdAndEntryType(1L, EntryType.DEBIT)).thenReturn(3000L);

        AccountBalanceResponse response = accountService.getBalance(1L);

        assertThat(response.getBalance()).isEqualTo(7000L); // 10000 - 3000
    }

    @Test
    @DisplayName("Balance is zero on a fresh account")
    void balanceIsZeroOnFreshAccount() {
        Account account = account(2L, "New Account");
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(entryRepository.sumByAccountIdAndEntryType(2L, EntryType.CREDIT)).thenReturn(0L);
        when(entryRepository.sumByAccountIdAndEntryType(2L, EntryType.DEBIT)).thenReturn(0L);

        assertThat(accountService.getBalance(2L).getBalance()).isZero();
    }

    @Test
    @DisplayName("Throws AccountNotFoundException for unknown account")
    void throwsForUnknownAccount() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getBalance(99L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("99");
    }

    private Account account(Long id, String name) {
        Account a = new Account();
        a.setId(id);
        a.setName(name);
        return a;
    }
}
