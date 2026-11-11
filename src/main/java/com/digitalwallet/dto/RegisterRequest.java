package com.digitalwallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest (

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name can't exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword

) {

}

