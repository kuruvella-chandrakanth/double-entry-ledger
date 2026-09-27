package com.nuplay.ledger.repository;

import com.nuplay.ledger.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Acquires a pessimistic write lock on the account row.
     * Used inside a transaction when posting entries to prevent
     * concurrent balance inconsistencies.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    /**
     * Acquires pessimistic write locks on multiple accounts in a single query.
     * Ordering by ID before locking prevents deadlocks between concurrent transactions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN :ids ORDER BY a.id ASC")
    List<Account> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
