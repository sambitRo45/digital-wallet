package com.digitalwallet.controller;

import com.digitalwallet.dto.TransactionResponse;
import com.digitalwallet.dto.WalletOperationRequest;
import com.digitalwallet.service.WalletTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletTransactionController {

    private final WalletTransactionService transactionService;

    public WalletTransactionController(WalletTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WalletOperationRequest request
            ) {

        TransactionResponse response = transactionService.deposit(
                authentication.getName(),
                idempotencyKey,
                request
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WalletOperationRequest request
    ) {

        TransactionResponse response = transactionService.withdraw(
                authentication.getName(),
                idempotencyKey,
                request
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
