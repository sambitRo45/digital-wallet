package com.digitalwallet.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(

        Long id,
        String actorEmail,
        String action,
        String resourceType,
        String resourceId,
        String reason,
        String details,
        LocalDateTime createdAt
) {
}
