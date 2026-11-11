package com.digitalwallet.dto;

import com.digitalwallet.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        String walletNumber,
        String ownerName,
        String ownerEmail,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        LocalDateTime createdAt
) {
}
