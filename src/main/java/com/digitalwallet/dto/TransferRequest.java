package com.digitalwallet.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Receiver wallet number is required")
        @Size(max = 40, message = "Invalid wallet number")
        String receiverWalletNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Transfer amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Amount can have at most 2 decimal places"
        )
        BigDecimal amount,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description


) {
}
