package com.digitalwallet.controller;

import com.digitalwallet.dto.TransferRequest;
import com.digitalwallet.dto.TransferResponse;
import com.digitalwallet.service.WalletTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletTransferController {

    private final WalletTransferService walletTransferService;

    public WalletTransferController(WalletTransferService walletTransferService) {
        this.walletTransferService = walletTransferService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
            ) {

        TransferResponse response = walletTransferService.transfer(
                authentication.getName(),
                idempotencyKey,
                request
        );

        return ResponseEntity.ok(response);
    }
}
