package com.nuplay.ledger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InvariantCheckResponse {

    private Long totalDebits;
    private Long totalCredits;
    private boolean balanced;
    private Instant checkedAt;
}
