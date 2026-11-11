package com.digitalwallet.service;

import com.digitalwallet.dto.AuthResponse;
import com.digitalwallet.dto.LoginRequest;
import com.digitalwallet.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
