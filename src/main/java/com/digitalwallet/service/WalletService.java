package com.digitalwallet.service;

import com.digitalwallet.dto.WalletBalanceResponse;
import com.digitalwallet.dto.WalletResponse;
import com.digitalwallet.entity.User;

public interface WalletService {

    void createWalletForUser(User user);

    WalletResponse getMyWallet(String email);

    WalletBalanceResponse getMyBalance(String email);
}
