package com.nuplay.ledger.controller;

import com.nuplay.ledger.dto.InvariantCheckResponse;
import com.nuplay.ledger.service.LedgerInvariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerInvariantService invariantService;

    /**
     * GET /api/ledger/verify
     *
     * Proves — live, from the database — that the double-entry invariant holds:
     *   sum(all DEBIT amounts) == sum(all CREDIT amounts)
     *
     * A balanced=true response is a real-time proof, not an assertion.
     * balanced=false would indicate a data integrity violation and should never occur.
     */
    @GetMapping("/verify")
    public InvariantCheckResponse verify() {
        return invariantService.verify();
    }
}
