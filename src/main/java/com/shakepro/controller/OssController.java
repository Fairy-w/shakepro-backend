package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.FileRecordRequest;
import com.shakepro.dto.request.PresignRequest;
import com.shakepro.dto.response.PresignResponse;
import com.shakepro.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "OSS", description = "文件上传相关接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @Operation(summary = "获取预签名上传URL")
    @PostMapping("/oss/presign")
    public ApiResponse<PresignResponse> presign(@Valid @RequestBody PresignRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(ossService.generatePresignUrl(request, userId));
    }

    @Operation(summary = "保存文件记录")
    @PostMapping("/files")
    public ApiResponse<Map<String, Long>> saveFile(@Valid @RequestBody FileRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long fileId = ossService.saveFileRecord(request, userId);
        return ApiResponse.success(Map.of("fileId", fileId));
    }
}
