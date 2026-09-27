package com.nuplay.ledger.repository;

import com.nuplay.ledger.model.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    /**
     * Used for idempotency check: if a transaction with this key already exists,
     * return it instead of creating a new one.
     */
    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);
}
