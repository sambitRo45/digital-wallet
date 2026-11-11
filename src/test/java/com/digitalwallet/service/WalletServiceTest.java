package com.digitalwallet.service;

import com.digitalwallet.entity.User;
import com.digitalwallet.entity.Wallet;
import com.digitalwallet.repository.WalletRepository;
import com.digitalwallet.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void getMyWalletShouldReturnWallet() {

        User user = new User();
        user.setFullName("Test User");
        user.setEmail("test@example.com");

        Wallet wallet = new Wallet();
        wallet.setWalletNumber("DW123456789");
        wallet.setOwner(user);

        when(walletRepository.findByOwner_Email("test@example.com")).thenReturn(Optional.of(wallet));

        var result = walletService.getMyWallet(
                "test@example.com"
        );

        assertNotNull(result);
        assertEquals("DW123456789", result.walletNumber());

        verify(walletRepository, times(1))
                .findByOwner_Email("test@example.com");
    }

    @Test
    void getMyBalanceShouldReturnZeroForNewWallet() {

        User user = new User();
        user.setFullName("Test User");
        user.setEmail("test@example.com");

        Wallet wallet = new Wallet();
        wallet.setWalletNumber("DW123456789");
        wallet.setOwner(user);

        when(walletRepository.findByOwner_Email(
                "test@example.com"
        )).thenReturn(Optional.of(wallet));

        var result = walletService.getMyBalance(
                "test@example.com"
        );

        assertNotNull(result);
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result.balance())
        );
    }
}
