package com.nuplay.ledger.dto;

import com.nuplay.ledger.model.LedgerEntry.EntryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTransactionRequest {

    @NotBlank
    private String idempotencyKey;

    private String description;

    @NotNull
    @Size(min = 2, message = "A transaction requires at least two entries")
    @Valid
    private List<EntryLine> entries;

    @Data
    public static class EntryLine {

        @NotNull
        private Long accountId;

        @NotNull
        private EntryType entryType;

        @NotNull
        @Positive
        private Long amount; // minor units (cents)
    }
}
