package com.digitalwallet.controller;

import com.digitalwallet.dto.UpdateProfileRequest;
import com.digitalwallet.dto.UserProfileResponse;
import com.digitalwallet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {

        return userService.getMyProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserProfileResponse updateMyProfile(Authentication authentication,
                                               @Valid @RequestBody UpdateProfileRequest request) {

        return userService.updateProfile(authentication.getName(), request);
    }
}
