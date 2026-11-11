package com.digitalwallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(

        String transferReference,

        String debitTransactionReference,

        String creditTransactionReference,

        String senderWalletNumber,

        String receiverWalletNumber,

        BigDecimal amount,

        String currency,

        BigDecimal senderBalanceAfter,

        BigDecimal receiverBalanceAfter,

        LocalDateTime createdAt
) {
}
