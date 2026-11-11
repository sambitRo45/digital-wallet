package com.digitalwallet.service;

import com.digitalwallet.dto.TransactionResponse;
import com.digitalwallet.dto.WalletOperationRequest;

public interface WalletTransactionService {

    TransactionResponse deposit(String email,
                                String idempotencyKey,
                                WalletOperationRequest request);

    TransactionResponse withdraw(String email,
                                 String idempotencyKey,
                                 WalletOperationRequest request);
}
