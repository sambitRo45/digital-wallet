package com.digitalwallet.controller;

import com.digitalwallet.dto.*;
import com.digitalwallet.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getAllUsers(page, size);
    }

    @GetMapping("/users/{id}")
    public AdminUserResponse getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse updateUserStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request
            ){
        return adminService.updateUserStatus(
                authentication.getName(),
                id,
                request
        );
    }

    @GetMapping("/wallets")
    public PageResponse<AdminWalletResponse> getAllWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getAllWallets(page, size);
    }

    @PatchMapping("/wallets/{id}/status")
    public AdminWalletResponse updateWalletStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request
    ) {
        return adminService.updateWalletStatus(
                authentication.getName(),
                id,
                request
        );
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getAuditLogs(page, size);
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionHistoryResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getAllTransactions(page, size);
    }

}
