package com.digitalwallet.service.impl;

import com.digitalwallet.dto.AuthResponse;
import com.digitalwallet.dto.LoginRequest;
import com.digitalwallet.dto.RegisterRequest;
import com.digitalwallet.entity.User;
import com.digitalwallet.enums.AccountStatus;
import com.digitalwallet.enums.Role;
import com.digitalwallet.repository.UserRepository;
import com.digitalwallet.security.JwtService;
import com.digitalwallet.service.AuthService;
import com.digitalwallet.service.WalletService;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final WalletService walletService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService, WalletService walletService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.walletService = walletService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if(!request.password().equals(request.confirmPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Password and Confirm password don't match"
            );
        }

        int passwordBytes = request.password()
                .getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is already register"
            );
        }

        walletService.createWalletForUser(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email, request.password()
                    )
            );
        } catch (DisabledException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is disabled"
            );
        }  catch (BadCredentialsException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication failed"
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs() / 1000
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
