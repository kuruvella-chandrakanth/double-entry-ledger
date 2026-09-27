package com.nuplay.ledger.dto;

import com.nuplay.ledger.model.LedgerEntry.EntryType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String idempotencyKey;
    private String description;
    private Instant createdAt;
    private List<EntryLineResponse> entries;

    @Data
    @Builder
    public static class EntryLineResponse {
        private Long accountId;
        private String accountName;
        private EntryType entryType;
        private Long amount;
    }
}
