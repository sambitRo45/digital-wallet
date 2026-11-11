package com.digitalwallet.dto;

import com.digitalwallet.enums.AccountStatus;
import com.digitalwallet.enums.Role;

import java.time.LocalDateTime;

public record UserProfileResponse (

        Long id,
        String fullName,
        String email,
        Role role,
        AccountStatus accountStatus,
        LocalDateTime createdAt

) {

}
