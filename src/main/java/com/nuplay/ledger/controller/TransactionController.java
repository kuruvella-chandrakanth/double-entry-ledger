package com.nuplay.ledger.controller;

import com.nuplay.ledger.dto.CreateTransactionRequest;
import com.nuplay.ledger.dto.TransactionResponse;
import com.nuplay.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions
     *
     * Posts a balanced double-entry transaction.
     * Idempotent: supplying the same idempotency_key a second time returns the
     * original transaction without moving money again.
     *
     * Returns HTTP 201 on a fresh commit.
     * Returns HTTP 200 (same body) if the idempotency_key was already processed.
     * Returns HTTP 422 if sum(DEBIT) != sum(CREDIT).
     * Returns HTTP 400 if the request body fails validation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse postTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.postTransaction(request);
    }

    /**
     * GET /api/transactions/{id}
     * Retrieves a previously posted transaction by its internal ID.
     */
    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        return transactionService.getTransaction(id);
    }
}
