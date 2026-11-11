package com.digitalwallet.service.impl;

import com.digitalwallet.dto.PageResponse;
import com.digitalwallet.dto.TransactionHistoryResponse;
import com.digitalwallet.entity.WalletTransaction;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;
import com.digitalwallet.repository.WalletTransactionRepository;
import com.digitalwallet.service.TransactionHistoryService;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "amount",
            "transactionReference",
            "transactionType",
            "status"
    );

    private final WalletTransactionRepository transactionRepository;

    public TransactionHistoryServiceImpl(WalletTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public PageResponse<TransactionHistoryResponse> getTransactions(String email, TransactionType type, TransactionStatus status, LocalDateTime fromDate, LocalDateTime toDate, int page, int size, String sortBy, String direction) {

        validateFilters(fromDate, toDate, page, size);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        String safeSortBy = sortBy == null || sortBy.isBlank()
                ? "createdAt" : sortBy;

        if (!ALLOWED_SORT_FIELDS.contains(safeSortBy)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort field"
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(
                    direction == null ? "DESC" : direction
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort direction. Use ASC or DESC"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, safeSortBy)
        );

        Page<WalletTransaction> transactions = transactionRepository.findTransactionHistory(
                normalizedEmail,
                type,
                status,
                fromDate,
                toDate,
                pageable
        );

        Page<TransactionHistoryResponse> responsePage =
                transactions.map(this::mapToResponse);

        return new PageResponse<>(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );

    }

    @Override
    public TransactionHistoryResponse getTransactionByReference(String email, String transactionReference) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        WalletTransaction transaction = transactionRepository
                .findByTransactionReferenceAndWallet_Owner_Email(
                        transactionReference,
                        normalizedEmail
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found"
                ));

        return mapToResponse(transaction);
    }

    private TransactionHistoryResponse mapToResponse(
            WalletTransaction transaction
    ) {
        return new TransactionHistoryResponse(
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getTransferReference(),
                transaction.getWallet().getWalletNumber(),
                transaction.getCreatedAt()
        );
    }

    private void validateFilters(
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "FromDate cannot be after toDate"
            );
        }
    }
}
