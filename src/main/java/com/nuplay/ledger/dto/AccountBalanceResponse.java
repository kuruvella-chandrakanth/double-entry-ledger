package com.nuplay.ledger.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountBalanceResponse {

    private Long accountId;
    private String name;
    // Credits increase balance, debits decrease it — like a bank account.
    // balance = sum(CREDIT) - sum(DEBIT)
    private Long balance;
}
