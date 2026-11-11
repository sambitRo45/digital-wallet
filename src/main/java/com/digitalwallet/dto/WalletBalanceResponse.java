package com.digitalwallet.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(

        String walletNumber,
        BigDecimal balance,
        String currency
) {
}
