package com.shakepro.service;

import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.request.RegisterRequest;
import com.shakepro.dto.response.LoginResponse;
import com.shakepro.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse getCurrentUser(Long userId);
}
