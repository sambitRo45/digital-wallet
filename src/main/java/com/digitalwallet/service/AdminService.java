package com.digitalwallet.service;

import com.digitalwallet.dto.*;

public interface AdminService {

    PageResponse<AdminUserResponse> getAllUsers(
            int page,
            int size
    );

    AdminUserResponse getUserById(Long userId);

    AdminUserResponse updateUserStatus(
            String adminEmail,
            Long userId,
            AdminStatusRequest request
    );

    PageResponse<AdminWalletResponse> getAllWallets(
            int page,
            int size
    );

    AdminWalletResponse updateWalletStatus(
            String adminEmail,
            Long userId,
            AdminStatusRequest request
    );

    PageResponse<AuditLogResponse> getAuditLogs(
            int page,
            int size
    );

    PageResponse<TransactionHistoryResponse> getAllTransactions(
            int page,
            int size
    );
}
