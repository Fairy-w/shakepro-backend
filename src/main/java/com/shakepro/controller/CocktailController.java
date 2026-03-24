package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.response.BannerResponse;
import com.shakepro.dto.response.CategoryResponse;
import com.shakepro.dto.response.CocktailDetailResponse;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.service.CocktailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cocktails", description = "鸡尾酒/配方相关接口")
@RestController
@RequestMapping("/api/cocktails")
@RequiredArgsConstructor
public class CocktailController {

    private final CocktailService cocktailService;

    @Operation(summary = "获取鸡尾酒列表（分页）")
    @GetMapping
    public ApiResponse<Page<CocktailListResponse>> list(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(cocktailService.listCocktails(keyword, page, size));
    }

    @Operation(summary = "获取鸡尾酒详情")
    @GetMapping("/{id}")
    public ApiResponse<CocktailDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(cocktailService.getCocktailDetail(id));
    }

    @Operation(summary = "获取轮播图数据")
    @GetMapping("/banner")
    public ApiResponse<List<BannerResponse>> banner() {
        return ApiResponse.success(cocktailService.getBanners());
    }

    @Operation(summary = "获取分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.success(cocktailService.getCategories());
    }
}
