package com.digitalwallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminStatusRequest(

        @NotNull(message = "Status is required")
        Boolean active,

        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason
) {
}
