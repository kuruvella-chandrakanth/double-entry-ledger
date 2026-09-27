package com.nuplay.ledger.controller;

import com.nuplay.ledger.dto.AccountBalanceResponse;
import com.nuplay.ledger.dto.CreateAccountRequest;
import com.nuplay.ledger.dto.StatementResponse;
import com.nuplay.ledger.model.Account;
import com.nuplay.ledger.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/accounts
     * Creates a new ledger account (wallet, cash pool, revenue bucket, etc.)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    /**
     * GET /api/accounts/{id}/balance
     * Returns the current balance of the account.
     * Balance = sum(DEBIT) - sum(CREDIT) for asset accounts, inverted for liabilities/equity.
     */
    @GetMapping("/{id}/balance")
    public AccountBalanceResponse getBalance(@PathVariable Long id) {
        return accountService.getBalance(id);
    }

    /**
     * GET /api/accounts/{id}/statement?page=0&size=50
     * Returns a paginated, chronologically ordered statement with running balance.
     * History is immutable — entries are append-only.
     */
    @GetMapping("/{id}/statement")
    public StatementResponse getStatement(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        return accountService.getStatement(id, page, size);
    }
}
