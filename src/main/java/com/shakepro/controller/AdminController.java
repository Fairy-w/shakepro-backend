package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.request.admin.AdminCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminMaterialSaveRequest;
import com.shakepro.dto.response.admin.AdminAiCocktailFavoriteResponse;
import com.shakepro.dto.response.admin.AdminCocktailDetailResponse;
import com.shakepro.dto.response.admin.AdminCocktailListResponse;
import com.shakepro.dto.response.admin.AdminDashboardResponse;
import com.shakepro.dto.response.admin.AdminMaterialResponse;
import com.shakepro.dto.response.admin.AdminUserResponse;
import com.shakepro.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin", description = "后台管理接口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "后台仪表盘")
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    @Operation(summary = "用户分页列表")
    @GetMapping("/users")
    public ApiResponse<Page<AdminUserResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listUsers(keyword, page, size));
    }

    @Operation(summary = "AI配方收藏分页列表")
    @GetMapping("/favorites/ai-cocktails")
    public ApiResponse<Page<AdminAiCocktailFavoriteResponse>> aiCocktailFavorites(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listAiCocktailFavorites(keyword, page, size));
    }

    @Operation(summary = "材料列表")
    @GetMapping("/materials")
    public ApiResponse<List<AdminMaterialResponse>> materials(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(adminService.listMaterials(keyword, category));
    }

    @Operation(summary = "新增材料")
    @PostMapping("/materials")
    public ApiResponse<AdminMaterialResponse> createMaterial(@Valid @RequestBody AdminMaterialSaveRequest request) {
        return ApiResponse.success(adminService.createMaterial(request));
    }

    @Operation(summary = "修改材料")
    @PutMapping("/materials/{id}")
    public ApiResponse<AdminMaterialResponse> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody AdminMaterialSaveRequest request) {
        return ApiResponse.success(adminService.updateMaterial(id, request));
    }

    @Operation(summary = "删除材料")
    @DeleteMapping("/materials/{id}")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        adminService.deleteMaterial(id);
        return ApiResponse.success();
    }

    @Operation(summary = "鸡尾酒分页列表")
    @GetMapping("/cocktails")
    public ApiResponse<Page<AdminCocktailListResponse>> cocktails(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listCocktails(keyword, page, size));
    }

    @Operation(summary = "鸡尾酒详情")
    @GetMapping("/cocktails/{id}")
    public ApiResponse<AdminCocktailDetailResponse> cocktail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getCocktail(id));
    }

    @Operation(summary = "新增鸡尾酒")
    @PostMapping("/cocktails")
    public ApiResponse<AdminCocktailDetailResponse> createCocktail(
            @Valid @RequestBody AdminCocktailSaveRequest request) {
        return ApiResponse.success(adminService.createCocktail(request));
    }

    @Operation(summary = "修改鸡尾酒")
    @PutMapping("/cocktails/{id}")
    public ApiResponse<AdminCocktailDetailResponse> updateCocktail(
            @PathVariable Long id,
            @Valid @RequestBody AdminCocktailSaveRequest request) {
        return ApiResponse.success(adminService.updateCocktail(id, request));
    }

    @Operation(summary = "删除鸡尾酒")
    @DeleteMapping("/cocktails/{id}")
    public ApiResponse<Void> deleteCocktail(@PathVariable Long id) {
        adminService.deleteCocktail(id);
        return ApiResponse.success();
    }

    @Operation(summary = "删除AI配方收藏")
    @DeleteMapping("/favorites/ai-cocktails/{id}")
    public ApiResponse<Void> deleteAiCocktailFavorite(@PathVariable Long id) {
        adminService.deleteAiCocktailFavorite(id);
        return ApiResponse.success();
    }
}
