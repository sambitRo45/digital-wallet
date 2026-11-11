package com.digitalwallet.service.impl;

import com.digitalwallet.dto.TransferRequest;
import com.digitalwallet.dto.TransferResponse;
import com.digitalwallet.entity.IdempotencyRecord;
import com.digitalwallet.entity.Wallet;
import com.digitalwallet.entity.WalletTransaction;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;
import com.digitalwallet.enums.WalletStatus;
import com.digitalwallet.repository.IdempotencyRepository;
import com.digitalwallet.repository.WalletRepository;
import com.digitalwallet.repository.WalletTransactionRepository;
import com.digitalwallet.service.WalletTransferService;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WalletTransferServiceImpl implements WalletTransferService {

    private static final String OPERATION = "TRANSFER";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public WalletTransferServiceImpl(WalletRepository walletRepository, WalletTransactionRepository transactionRepository, IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TransferResponse transfer(String senderEmail, String idempotencyKey, TransferRequest request) {

        validateIdempotencyKey(idempotencyKey);

        String email = senderEmail.trim().toLowerCase(Locale.ROOT);
        String receiverNumber = request.receiverWalletNumber().trim();

        // Find the two wallets before acquiring locks.
        Wallet senderReference = walletRepository
                .findByOwner_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sender wallet not found"
                ));

        Wallet receiverReference = walletRepository
                .findByWalletNumber(receiverNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Receiver wallet not found"
                ));

        if (senderReference.getId().equals(receiverReference.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot transfer money to your own wallet"
            );
        }

        // Always acquire wallet locks in ascending ID order.
        List<Long> walletIds = List.of(
                senderReference.getId(),
                receiverReference.getId()
        ).stream().sorted().toList();

        Wallet firstWallet = lockWallet(walletIds.get(0));
        Wallet secondWallet = lockWallet(walletIds.get(1));

        Wallet sender = firstWallet.getId().equals(senderReference.getId())
                ? firstWallet : secondWallet;
        Wallet receiver = firstWallet.getId().equals(receiverReference.getId())
                ? firstWallet : secondWallet;


        if (sender.getStatus() != WalletStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Sender wallet is not active"
            );
        }

        if (receiver.getStatus() != WalletStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Receiver wallet is not active"
            );
        }

        String requestHash = generateRequestHash(request);

        var existingRecord = idempotencyRepository
                .findByUser_IdAndOperationAndIdempotencyKey(
                        sender.getOwner().getId(),
                        OPERATION,
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {

            IdempotencyRecord record = existingRecord.get();

            if (!record.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key was already used with different request"
                );
            }

            return deserializeResponse(record.getResponseBody());
        }

        BigDecimal amount = request.amount();

        // Check sender's available balance.
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient wallet balance"
            );
        }

        sender.debit(amount);
        receiver.credit(amount);

        walletRepository.save(sender);
        walletRepository.save(receiver);

        // Generate one reference for the complete transfer.
        String transferReference = generateReference("TRF");

        // Create sender's debit transaction.
        WalletTransaction debitTransaction = createTransaction(
                sender,
                TransactionType.TRANSFER_DEBIT,
                amount,
                request.description(),
                transferReference
        );

        // Create receiver's debit transaction.
        WalletTransaction creditTransaction = createTransaction(
                receiver,
                TransactionType.TRANSFER_CREDIT,
                amount,
                request.description(),
                transferReference
        );

        debitTransaction = transactionRepository.save(debitTransaction);
        creditTransaction = transactionRepository.save(creditTransaction);

        TransferResponse response = new TransferResponse(
                transferReference,
                debitTransaction.getTransactionReference(),
                creditTransaction.getTransactionReference(),
                sender.getWalletNumber(),
                receiver.getWalletNumber(),
                amount,
                sender.getCurrency(),
                sender.getBalance(),
                receiver.getBalance(),
                debitTransaction.getCreatedAt()
        );

        // Store the successful response for idempotent retries.
        IdempotencyRecord record = new IdempotencyRecord();

        record.setUser(sender.getOwner());
        record.setOperation(OPERATION);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setResponseStatus(HttpStatus.OK.value());
        record.setResponseBody(serializeResponse(response));

        idempotencyRepository.save(record);

        return response;
    }

    private Wallet lockWallet(Long walletId) {

        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));
    }

    private WalletTransaction createTransaction(Wallet wallet, TransactionType type, BigDecimal amount, String description, String transferReference) {

        WalletTransaction transaction = new WalletTransaction();

        transaction.setTransactionReference(generateReference("TXN"));
        transaction.setWallet(wallet);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(wallet.getCurrency());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        transaction.setTransferReference(transferReference);

        return transaction;
    }

    private String generateReference(String prefix) {

        return prefix + UUID.randomUUID()
                .toString()
                .replace("-","")
                .substring(0, 20)
                .toUpperCase(Locale.ROOT);
    }

    private void validateIdempotencyKey(String key) {

        if (key == null || key.isBlank() || key.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid Idempotency key header is required"
            );
        }
    }

    private String generateRequestHash(TransferRequest request) {

        String amount = request.amount()
                .stripTrailingZeros()
                .toPlainString();

        String description = request.description() == null ? "" : request.description().trim();

        String data = request.receiverWalletNumber().trim()
                + "|" + amount
                + "|" + description;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    data
                            .getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to generate request hash",
                    exception
            );
        }
    }

    private String serializeResponse(TransferResponse response) {

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw  new IllegalStateException(
                    "Unable to save transfer response"
            );
        }
    }

    private TransferResponse deserializeResponse(String responseBody) {
        try {
            return objectMapper.readValue(
                    responseBody,
                    TransferResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to retrieve previous transfer response",
                    exception
            );
        }
    }
}
