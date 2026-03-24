package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorites", description = "收藏相关接口")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
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
}
