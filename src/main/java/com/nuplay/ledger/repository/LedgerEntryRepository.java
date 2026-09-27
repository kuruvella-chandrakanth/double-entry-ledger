package com.nuplay.ledger.repository;

import com.nuplay.ledger.model.LedgerEntry;
import com.nuplay.ledger.model.LedgerEntry.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Fetch all entries for a given account, ordered chronologically.
     * Used to build the statement view with a running balance.
     */
    @Query("SELECT e FROM LedgerEntry e WHERE e.account.id = :accountId ORDER BY e.createdAt ASC, e.id ASC")
    Page<LedgerEntry> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Sum of all amounts for a given account and entry type.
     * Used to compute the account balance:
     *   balance = SUM(CREDIT) - SUM(DEBIT)   [for ASSET accounts it's SUM(DEBIT) - SUM(CREDIT)]
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e WHERE e.account.id = :accountId AND e.entryType = :entryType")
    Long sumByAccountIdAndEntryType(@Param("accountId") Long accountId, @Param("entryType") EntryType entryType);

    /**
     * Global sum of all DEBIT or CREDIT entries across the entire ledger.
     * Used by the invariant-check endpoint to prove sum(debits) == sum(credits).
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e WHERE e.entryType = :entryType")
    Long sumAllByEntryType(@Param("entryType") EntryType entryType);
}
