package com.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name can't exceed 100 characters")
        String fullName
) {
}
