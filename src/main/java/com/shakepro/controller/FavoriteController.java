package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.AiCocktailFavoriteCreateRequest;
import com.shakepro.dto.response.AiCocktailFavoriteActionResponse;
import com.shakepro.dto.response.AiCocktailFavoritePageResponse;
import com.shakepro.dto.response.AiCocktailFavoriteStatusResponse;
import com.shakepro.dto.response.CocktailListResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.shakepro.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorites", description = "收藏相关接口")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Validated
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏鸡尾酒")
    @PostMapping("/{cocktailId}")
    public ApiResponse<Void> addFavorite(@PathVariable Long cocktailId) {
        Long userId = SecurityUtils.getCurrentUserId();
        favoriteService.addFavorite(userId, cocktailId);
        return ApiResponse.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{cocktailId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long cocktailId) {
        Long userId = SecurityUtils.getCurrentUserId();
        favoriteService.removeFavorite(userId, cocktailId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping
    public ApiResponse<List<CocktailListResponse>> listFavorites() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.listFavorites(userId));
    }

    @Operation(summary = "收藏AI生成配方")
    @PostMapping("/ai-cocktails")
    public ApiResponse<AiCocktailFavoriteActionResponse> addAiCocktailFavorite(
            @Valid @RequestBody AiCocktailFavoriteCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.addAiCocktailFavorite(userId, request));
    }

    @Operation(summary = "取消收藏AI生成配方（按收藏ID）")
    @DeleteMapping("/ai-cocktails/{favoriteId}")
    public ApiResponse<AiCocktailFavoriteActionResponse> removeAiCocktailFavorite(@PathVariable Long favoriteId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.removeAiCocktailFavorite(userId, favoriteId));
    }

    @Operation(summary = "取消收藏AI生成配方（按recipeKey）")
    @DeleteMapping("/ai-cocktails")
    public ApiResponse<AiCocktailFavoriteActionResponse> removeAiCocktailFavoriteByRecipeKey(
            @Parameter(description = "配方唯一键") @RequestParam @NotBlank String recipeKey) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.removeAiCocktailFavoriteByRecipeKey(userId, recipeKey));
    }

    @Operation(summary = "获取我的AI配方收藏列表")
    @GetMapping("/ai-cocktails")
    public ApiResponse<AiCocktailFavoritePageResponse> listAiCocktailFavorites(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @Min(1) int pageSize,
            @Parameter(description = "关键字，可搜索名称/描述/prompt") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序，示例 createdAt,desc") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.listAiCocktailFavorites(userId, keyword, pageNo, pageSize, sort));
    }

    @Operation(summary = "查询AI生成配方是否已收藏")
    @GetMapping("/ai-cocktails/status")
    public ApiResponse<AiCocktailFavoriteStatusResponse> getAiCocktailFavoriteStatus(
            @Parameter(description = "配方唯一键") @RequestParam @NotBlank String recipeKey) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(favoriteService.getAiCocktailFavoriteStatus(userId, recipeKey));
    }
}
