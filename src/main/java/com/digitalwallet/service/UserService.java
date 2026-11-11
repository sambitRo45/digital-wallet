package com.digitalwallet.service;

import com.digitalwallet.dto.UpdateProfileRequest;
import com.digitalwallet.dto.UserProfileResponse;


public interface UserService {

    UserProfileResponse getMyProfile(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
}
