package com.investotech.accounttransfertask.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTransferRequest(
        @JsonProperty("source_account_id")
        @NotBlank @Size(max = 100)
        String sourceAccountId,

        @JsonProperty("destination_account_id")
        @NotBlank @Size(max = 100)
        String destinationAccountId,

        @JsonProperty("amount")
        @JsonAlias("amoun")
        @Positive
        int amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase code")
        String currency
) {
}
