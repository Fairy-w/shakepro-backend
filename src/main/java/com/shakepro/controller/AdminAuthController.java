package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.response.admin.AdminLoginResponse;
import com.shakepro.dto.response.admin.AdminProfileResponse;
import com.shakepro.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Auth", description = "后台管理员认证接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(adminService.login(request));
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/me")
    public ApiResponse<AdminProfileResponse> me() {
        return ApiResponse.success(adminService.getCurrentAdmin(SecurityUtils.getCurrentUserId()));
    }
}
