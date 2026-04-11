package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.response.AuthConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App Config", description = "应用配置相关接口")
@RestController
@RequestMapping("/api/app/config")
public class AppConfigController {

    @Operation(summary = "获取认证页面配置")
    @GetMapping("/auth")
    public ApiResponse<AuthConfigResponse> getAuthConfig() {
        return ApiResponse.<AuthConfigResponse>builder()
                .code(HttpStatus.OK.value())
                .message("成功")
                .data(AuthConfigResponse.defaultConfig())
                .build();
    }
}
