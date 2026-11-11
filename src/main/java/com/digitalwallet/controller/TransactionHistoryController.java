package com.digitalwallet.controller;

import com.digitalwallet.dto.PageResponse;
import com.digitalwallet.dto.TransactionHistoryResponse;
import com.digitalwallet.enums.TransactionStatus;
import com.digitalwallet.enums.TransactionType;
import com.digitalwallet.service.TransactionHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/wallet/transactions")
public class TransactionHistoryController {

    private final TransactionHistoryService historyService;

    public TransactionHistoryController(TransactionHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public PageResponse<TransactionHistoryResponse> getTransactions(

            Authentication authentication,

            @RequestParam(required = false)
            TransactionType type,

            @RequestParam(required = false)
            TransactionStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String direction
    ) {

        return historyService.getTransactions(
                authentication.getName(),
                type,
                status,
                fromDate,
                toDate,
                page,
                size,
                sortBy,
                direction
        );
    }

    @GetMapping("/{reference}")
    public TransactionHistoryResponse getTransactionHistoryByReference(Authentication authentication,
                                                                       @PathVariable String reference
                                                                       ) {
        return historyService.getTransactionByReference(
                authentication.getName(),
                reference
        );
    }
}
