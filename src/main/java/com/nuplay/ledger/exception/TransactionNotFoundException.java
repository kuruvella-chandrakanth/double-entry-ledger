package com.nuplay.ledger.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found: id=" + transactionId);
    }
}
