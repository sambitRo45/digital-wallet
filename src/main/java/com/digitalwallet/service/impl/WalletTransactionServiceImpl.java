package com.digitalwallet.service.impl;

import com.digitalwallet.dto.TransactionResponse;
import com.digitalwallet.dto.WalletOperationRequest;
import com.digitalwallet.entity.IdempotencyRecord;
import com.digitalwallet.entity.Wallet;
import com.digitalwallet.entity.WalletTransaction;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;
import com.digitalwallet.enums.WalletStatus;
import com.digitalwallet.repository.IdempotencyRepository;
import com.digitalwallet.repository.WalletRepository;
import com.digitalwallet.repository.WalletTransactionRepository;
import com.digitalwallet.service.WalletTransactionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class WalletTransactionServiceImpl
        implements WalletTransactionService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public WalletTransactionServiceImpl(
            WalletRepository walletRepository,
            WalletTransactionRepository transactionRepository,
            IdempotencyRepository idempotencyRepository,
            ObjectMapper objectMapper
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransactionResponse deposit(
            String email,
            String idempotencyKey,
            WalletOperationRequest request
    ) {
        return executeOperation(
                email,
                idempotencyKey,
                request,
                TransactionType.DEPOSIT
        );
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(
            String email,
            String idempotencyKey,
            WalletOperationRequest request
    ) {
        return executeOperation(
                email,
                idempotencyKey,
                request,
                TransactionType.WITHDRAWAL
        );
    }

    private TransactionResponse executeOperation(
            String email,
            String idempotencyKey,
            WalletOperationRequest request,
            TransactionType transactionType
    ) {
        validateIdempotencyKey(idempotencyKey);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String operation = transactionType.name();

        // Lock this wallet row until the database transaction completes.
        Wallet wallet = walletRepository
                .findByOwnerEmailForUpdate(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Wallet is not active"
            );
        }

        String requestHash = generateRequestHash(
                transactionType,
                request
        );

        // Check whether this operation was already processed.
        var existingRecord = idempotencyRepository
                .findByUser_IdAndOperationAndIdempotencyKey(
                        wallet.getOwner().getId(),
                        operation,
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();

            if (!record.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key was already used with a different request"
                );
            }

            return deserializeResponse(record.getResponseBody());
        }

        BigDecimal amount = request.amount();

        // Update the wallet balance.
        if (transactionType == TransactionType.DEPOSIT) {
            wallet.credit(amount);
        } else {
            try {
                wallet.debit(amount);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Insufficient wallet balance"
                );
            }
        }

        walletRepository.save(wallet);

        // Create the transaction ledger entry.
        WalletTransaction transaction = new WalletTransaction();

        transaction.setTransactionReference(
                "TXN" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 20)
                        .toUpperCase(Locale.ROOT)
        );

        transaction.setWallet(wallet);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setCurrency(wallet.getCurrency());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.description());

        transaction = transactionRepository.save(transaction);

        TransactionResponse response = new TransactionResponse(
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                wallet.getBalance(),
                transaction.getCreatedAt()
        );

        // Save the successful response for future retries.
        IdempotencyRecord record = new IdempotencyRecord();

        record.setUser(wallet.getOwner());
        record.setOperation(operation);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setResponseStatus(HttpStatus.OK.value());
        record.setResponseBody(serializeResponse(response));

        idempotencyRepository.save(record);

        return response;
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid Idempotency-Key header is required (maximum 100 characters)"
            );
        }
    }

    private String generateRequestHash(
            TransactionType type,
            WalletOperationRequest request
    ) {
        String amount = request.amount()
                .stripTrailingZeros()
                .toPlainString();

        String description = request.description() == null
                ? ""
                : request.description().trim();

        String requestData = type.name()
                + "|" + amount
                + "|" + description;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    requestData.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to generate request hash",
                    exception
            );
        }
    }

    private String serializeResponse(TransactionResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to save transaction response",
                    exception
            );
        }
    }

    private TransactionResponse deserializeResponse(String responseBody) {
        try {
            return objectMapper.readValue(
                    responseBody,
                    TransactionResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to retrieve previous transaction response",
                    exception
            );
        }
    }
}