package com.digitalwallet.service.impl;

import com.digitalwallet.dto.WalletBalanceResponse;
import com.digitalwallet.dto.WalletResponse;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.Wallet;
import com.digitalwallet.enums.WalletStatus;
import com.digitalwallet.repository.WalletRepository;
import com.digitalwallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    public WalletServiceImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public void createWalletForUser(User user) {

        if(walletRepository.existsByOwner_Id(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already has a wallet"
            );
        }

        Wallet wallet = new Wallet();
        wallet.setWalletNumber(generateWalletNumber());
        wallet.setOwner(user);
        wallet.setStatus(WalletStatus.ACTIVE);

        walletRepository.save(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {

        Wallet wallet = findWalletByEmail(email);

        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getOwner().getFullName(),
                wallet.getOwner().getEmail(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public WalletBalanceResponse getMyBalance(String email) {

        Wallet wallet = findWalletByEmail(email);

        return new WalletBalanceResponse(
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getCurrency()
        );
    }

    private Wallet findWalletByEmail(String email) {

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return walletRepository.findByOwner_Email(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet Not Found"
                ));
    }

    private String generateWalletNumber() {

        return "DW" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase(Locale.ROOT);
    }
}
