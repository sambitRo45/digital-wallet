package com.digitalwallet.config;

import com.digitalwallet.entity.User;
import com.digitalwallet.enums.AccountStatus;
import com.digitalwallet.enums.Role;
import com.digitalwallet.repository.UserRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {

        if (adminEmail == null || adminEmail.isBlank() ||
            adminPassword == null || adminPassword.isBlank()
        ) {
            throw new IllegalStateException(
                    "Admin email and password must be configured"
            );
        }

        String email = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {

            User existingUser = userRepository.findByEmail(email)
                    .orElseThrow();

            if (existingUser.getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Configured admin email already belongs to a non-admin user."
                );
            }

            log.info("Admin account already exist. Skipping creation.");
            return;
        }

        User admin = User.builder()
                .fullName(adminFullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userRepository.save(admin);

        log.info("Default admin account created successfully.");
    }

}
