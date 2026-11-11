package com.digitalwallet.service;

import com.digitalwallet.dto.TransferRequest;
import com.digitalwallet.dto.TransferResponse;

public interface WalletTransferService {

    TransferResponse transfer(
            String senderEmail,
            String idempotencyKey,
            TransferRequest request
    );
}
