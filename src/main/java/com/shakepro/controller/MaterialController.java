package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.response.MaterialResponse;
import com.shakepro.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Materials", description = "材料相关接口")
@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "获取材料列表")
    @GetMapping
    public ApiResponse<List<MaterialResponse>> list(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(materialService.listMaterials(keyword));
    }

    @Operation(summary = "获取材料分类")
    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(materialService.listCategories());
    }
}
