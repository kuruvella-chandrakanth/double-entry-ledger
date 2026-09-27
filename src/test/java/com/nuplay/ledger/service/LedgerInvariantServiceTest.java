package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.InvariantCheckResponse;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerInvariantServiceTest {

    @Mock private LedgerEntryRepository entryRepository;
    @InjectMocks private LedgerInvariantService invariantService;

    @Test
    @DisplayName("Returns balanced=true when sum(DEBIT) == sum(CREDIT)")
    void balancedWhenInvariantHolds() {
        when(entryRepository.sumAllByEntryType(EntryType.DEBIT)).thenReturn(150_000L);
        when(entryRepository.sumAllByEntryType(EntryType.CREDIT)).thenReturn(150_000L);

        InvariantCheckResponse response = invariantService.verify();

        assertThat(response.isBalanced()).isTrue();
        assertThat(response.getTotalDebits()).isEqualTo(150_000L);
        assertThat(response.getTotalCredits()).isEqualTo(150_000L);
    }

    @Test
    @DisplayName("Returns balanced=false when invariant is violated")
    void imbalancedWhenInvariantViolated() {
        when(entryRepository.sumAllByEntryType(EntryType.DEBIT)).thenReturn(150_001L);
        when(entryRepository.sumAllByEntryType(EntryType.CREDIT)).thenReturn(150_000L);

        assertThat(invariantService.verify().isBalanced()).isFalse();
    }

    @Test
    @DisplayName("Returns balanced=true on empty ledger")
    void balancedOnEmptyLedger() {
        when(entryRepository.sumAllByEntryType(EntryType.DEBIT)).thenReturn(0L);
        when(entryRepository.sumAllByEntryType(EntryType.CREDIT)).thenReturn(0L);

        assertThat(invariantService.verify().isBalanced()).isTrue();
    }
}
