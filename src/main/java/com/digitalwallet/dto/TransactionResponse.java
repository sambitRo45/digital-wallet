package com.digitalwallet.dto;

import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(

        String transactionReference,
        TransactionType transactionType,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {
}
