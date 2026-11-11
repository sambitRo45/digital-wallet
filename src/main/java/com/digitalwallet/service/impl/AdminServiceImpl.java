package com.digitalwallet.service.impl;

import com.digitalwallet.dto.*;
import com.digitalwallet.entity.AuditLog;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.Wallet;
import com.digitalwallet.enums.AccountStatus;
import com.digitalwallet.enums.WalletStatus;
import com.digitalwallet.repository.AuditLogRepository;
import com.digitalwallet.repository.UserRepository;
import com.digitalwallet.repository.WalletRepository;
import com.digitalwallet.repository.WalletTransactionRepository;
import com.digitalwallet.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuditLogRepository auditLogRepository;
    private final WalletTransactionRepository transactionRepository;

    public AdminServiceImpl(UserRepository userRepository, WalletRepository walletRepository, AuditLogRepository auditLogRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(int page, int size) {
        Pageable pageable = createPageable(page, size);

        Page<AdminUserResponse> result = userRepository.findAll(pageable)
                .map(this::mapUser);

        return toPageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = findUser(userId);

        return mapUser(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserStatus(String adminEmail, Long userId, AdminStatusRequest request) {

        User user = findUser(userId);

        AccountStatus newStatus = request.active()
                ? AccountStatus.ACTIVE : AccountStatus.SUSPENDED;

        // Avoid accidentally locking the current administrator out.
        if (adminEmail.equalsIgnoreCase(user.getEmail()) && newStatus == AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot suspend your own account"
            );
        }

        AccountStatus oldStatus = user.getAccountStatus();

        if (oldStatus != newStatus) {
            user.setAccountStatus(newStatus);
            userRepository.save(user);

            User admin = findAdmin(adminEmail);

            saveAuditLog(
                    admin,
                    "USER_STATUS_CHANGED",
                    "USER",
                    user.getId().toString(),
                    request.reason(),
                    "{\"oldStatus\":\"" + oldStatus
                    + "\",\"newStatus\":\"" + newStatus + "\"}"
            );
        }
        return mapUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminWalletResponse> getAllWallets(int page, int size) {

        Pageable pageable = createPageable(page, size);

        Page<AdminWalletResponse> result = walletRepository.findAll(pageable)
                .map(this::mapWallet);

        return toPageResponse(result);
    }

    @Override
    @Transactional
    public AdminWalletResponse updateWalletStatus(String adminEmail, Long userId, AdminStatusRequest request) {

        Wallet wallet = walletRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wallet not found"
                ));

        WalletStatus newStatus = request.active()
                ? WalletStatus.ACTIVE : WalletStatus.FROZEN;

        WalletStatus oldStatus = wallet.getStatus();

        if (oldStatus != newStatus) {
            wallet.setStatus(newStatus);
            walletRepository.save(wallet);

            User admin = findAdmin(adminEmail);

            saveAuditLog(
                    admin,
                    "WALLET_STATUS_CHANGED",
                    "WALLET",
                    wallet.getId().toString(),
                    request.reason(),
                    "{\"oldStatus\":\"" + oldStatus
                            + "\",\"newStatus\":\"" + newStatus + "\"}"
            );
        }
        return mapWallet(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(int page, int size) {
        Pageable pageable = createPageable(page, size);

        Page<AuditLogResponse> result = auditLogRepository.findAll(pageable)
                .map(this::mapAuditLog);

        return toPageResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryResponse> getAllTransactions(
            int page,
            int size
    ) {
        Pageable pageable = createPageable(page, size);

        Page<TransactionHistoryResponse> result =
                transactionRepository.findAllTransactions(pageable)
                        .map(transaction -> new TransactionHistoryResponse(
                                transaction.getTransactionReference(),
                                transaction.getTransactionType(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getStatus(),
                                transaction.getDescription(),
                                transaction.getTransferReference(),
                                transaction.getWallet().getWalletNumber(),
                                transaction.getCreatedAt()
                        ));

        return toPageResponse(result);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private User findAdmin(String email) {
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Administrator not found"
                ));

        if (!admin.getRole().name().equals("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator access required"
            );
        }

        return admin;
    }

    private void saveAuditLog(
            User admin,
            String action,
            String resourceType,
            String resourceId,
            String reason,
            String details
    ) {
        AuditLog log = new AuditLog();

        log.setActor(admin);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setReason(reason);
        log.setDetails(details);

        auditLogRepository.save(log);
    }

    private AdminUserResponse mapUser(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }

    private AdminWalletResponse mapWallet(Wallet wallet) {
        User owner = wallet.getOwner();

        return new AdminWalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                owner.getFullName(),
                owner.getEmail(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }

    private AuditLogResponse mapAuditLog(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() == null ? null : log.getActor().getEmail(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getReason(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }

    private Pageable createPageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
