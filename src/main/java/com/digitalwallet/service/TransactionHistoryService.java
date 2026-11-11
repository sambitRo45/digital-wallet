package com.digitalwallet.service;

import com.digitalwallet.dto.PageResponse;
import com.digitalwallet.dto.TransactionHistoryResponse;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;

import java.time.LocalDateTime;

public interface TransactionHistoryService {

    PageResponse<TransactionHistoryResponse> getTransactions(
            String email,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size,
            String sortBy,
            String direction
    );

    TransactionHistoryResponse getTransactionByReference(
            String email,
            String transactionReference
    );
}
