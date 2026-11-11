package com.digitalwallet.controller;

import com.digitalwallet.dto.WalletBalanceResponse;
import com.digitalwallet.dto.WalletResponse;
import com.digitalwallet.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public WalletResponse getMyWallet(Authentication authentication) {

        return walletService.getMyWallet(authentication.getName());
    }

    @GetMapping("/me/balance")
    public WalletBalanceResponse getMyBalance(Authentication authentication) {

        return walletService.getMyBalance(authentication.getName());
    }
}
