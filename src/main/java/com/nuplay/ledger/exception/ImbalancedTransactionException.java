package com.nuplay.ledger.exception;

/**
 * Thrown when the entries in a transaction request do not balance.
 * sum(DEBIT amounts) must equal sum(CREDIT amounts) — if not, the transaction
 * is rejected before touching the database.
 */
public class ImbalancedTransactionException extends RuntimeException {

    public ImbalancedTransactionException(long totalDebits, long totalCredits) {
        super(String.format(
            "Transaction is imbalanced: total debits=%d, total credits=%d (difference=%d). " +
            "sum(DEBIT amounts) must equal sum(CREDIT amounts).",
            totalDebits, totalCredits, Math.abs(totalDebits - totalCredits)
        ));
    }
}
