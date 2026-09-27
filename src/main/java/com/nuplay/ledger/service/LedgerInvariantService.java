package com.nuplay.ledger.service;

import com.nuplay.ledger.dto.InvariantCheckResponse;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import com.nuplay.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerInvariantService {

    private final LedgerEntryRepository entryRepository;

    // Live proof from the DB — not an assertion.
    // sum(all DEBITs) must equal sum(all CREDITs) at all times.
    @Transactional(readOnly = true)
    public InvariantCheckResponse verify() {
        long totalDebits  = entryRepository.sumAllByEntryType(EntryType.DEBIT);
        long totalCredits = entryRepository.sumAllByEntryType(EntryType.CREDIT);

        return InvariantCheckResponse.builder()
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .balanced(totalDebits == totalCredits)
            .checkedAt(Instant.now())
            .build();
    }
}
