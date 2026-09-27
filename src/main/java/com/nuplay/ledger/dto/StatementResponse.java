package com.nuplay.ledger.dto;

import com.nuplay.ledger.model.LedgerEntry.EntryType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StatementResponse {

    private Long accountId;
    private String accountName;
    private List<StatementEntry> entries;

    @Data
    @Builder
    public static class StatementEntry {
        private Long entryId;
        private Long transactionId;
        private EntryType entryType;
        private Long amount;
        private Long runningBalance;
        private String description;
        private Instant createdAt;
    }
}
