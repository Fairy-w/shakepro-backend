package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.UserMaterialManualBatchSaveRequest;
import com.shakepro.dto.request.UserMaterialManualSaveRequest;
import com.shakepro.dto.request.UserMaterialSaveRequest;
import com.shakepro.dto.response.UserMaterialResponse;
import com.shakepro.service.UserMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UserMaterials", description = "用户材料有无标记接口")
@RestController
@RequestMapping("/api/user-materials")
@RequiredArgsConstructor
@Validated
public class UserMaterialController {

    private final UserMaterialService userMaterialService;

    @Operation(summary = "保存/更新用户的扫码材料")
    @PostMapping
    public ApiResponse<UserMaterialResponse> saveFromScan(@Valid @RequestBody UserMaterialSaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(userMaterialService.saveFromScan(userId, request));
    }

    @Operation(summary = "手动新增/更新用户材料")
    @PostMapping("/manual")
    public ApiResponse<UserMaterialResponse> saveManual(@Valid @RequestBody UserMaterialManualSaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(userMaterialService.saveManual(userId, request));
    }

    @Operation(summary = "批量手动新增/更新用户材料")
    @PostMapping("/manual/batch")
    public ApiResponse<List<UserMaterialResponse>> saveManualBatch(@Valid @RequestBody UserMaterialManualBatchSaveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(userMaterialService.saveManualBatch(userId, request.getItems()));
    }

    @Operation(summary = "获取当前用户材料列表")
    @GetMapping
    public ApiResponse<List<UserMaterialResponse>> list(
            @Parameter(description = "关键字（按名称搜索）") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) String categoryId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(userMaterialService.list(userId, keyword, categoryId));
    }

    @Operation(summary = "删除当前用户的材料项（按条码）")
    @DeleteMapping("/{barcode}")
    public ApiResponse<Void> removeByBarcode(@PathVariable @NotBlank String barcode) {
        Long userId = SecurityUtils.getCurrentUserId();
        userMaterialService.removeByBarcode(userId, barcode);
        return ApiResponse.success();
    }
}
