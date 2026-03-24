package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.request.RegisterRequest;
import com.shakepro.dto.response.LoginResponse;
import com.shakepro.dto.response.UserResponse;
import com.shakepro.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "用户认证相关接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/auth/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(authService.getCurrentUser(userId));
    }
}
