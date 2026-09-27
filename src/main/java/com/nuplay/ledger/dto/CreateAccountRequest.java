package com.nuplay.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank
    private String name;
}
