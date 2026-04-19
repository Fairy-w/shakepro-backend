package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.request.admin.AdminCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminGeneratedCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminMaterialSaveRequest;
import com.shakepro.dto.request.admin.AdminPageAiGenerateRequest;
import com.shakepro.dto.request.admin.AdminPageBatchImportRequest;
import com.shakepro.dto.request.admin.AdminPageCrawlRequest;
import com.shakepro.dto.request.admin.AdminPageFieldExtractRequest;
import com.shakepro.dto.request.admin.AdminMaterialSyncRequest;
import com.shakepro.dto.response.UserMaterialResponse;
import com.shakepro.dto.response.admin.AdminBatchImportHistoryResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStartResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStatusResponse;
import com.shakepro.dto.response.admin.AdminAiCocktailFavoriteResponse;
import com.shakepro.dto.response.admin.AdminCocktailDetailResponse;
import com.shakepro.dto.response.admin.AdminCocktailListResponse;
import com.shakepro.dto.response.admin.AdminDashboardResponse;
import com.shakepro.dto.response.admin.AdminMaterialResponse;
import com.shakepro.dto.response.admin.AdminMaterialSyncResponse;
import com.shakepro.dto.response.admin.AdminPageBatchImportResponse;
import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;
import com.shakepro.dto.response.admin.AdminPageResult;
import com.shakepro.dto.response.admin.AdminPageTextResponse;
import com.shakepro.dto.response.admin.AdminUserResponse;
import com.shakepro.service.AdminPageAiGenerateService;
import com.shakepro.service.AdminPageBatchImportService;
import com.shakepro.service.AdminPageCrawlService;
import com.shakepro.service.AdminPageExtractService;
import com.shakepro.service.AdminService;
import com.shakepro.service.AdminMaterialSyncService;
import com.shakepro.service.AdminUserMaterialManageService;
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
    private final AdminPageCrawlService adminPageCrawlService;
    private final AdminPageExtractService adminPageExtractService;
    private final AdminPageAiGenerateService adminPageAiGenerateService;
    private final AdminPageBatchImportService adminPageBatchImportService;
    private final AdminUserMaterialManageService adminUserMaterialManageService;
    private final AdminMaterialSyncService adminMaterialSyncService;

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

    @Operation(summary = "同步TheCocktailDB基础材料到本地并上传图片到OSS")
    @PostMapping("/materials/sync/cocktaildb")
    public ApiResponse<AdminMaterialSyncResponse> syncMaterialsFromCocktailDb(
            @RequestBody(required = false) AdminMaterialSyncRequest request) {
        return ApiResponse.success(adminMaterialSyncService.syncFromCocktailDb(request));
    }

    @Operation(summary = "管理员获取指定用户材料列表")
    @GetMapping("/user-materials")
    public ApiResponse<List<UserMaterialResponse>> userMaterials(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId) {
        return ApiResponse.success(adminUserMaterialManageService.list(userId, keyword, categoryId));
    }

    @Operation(summary = "鸡尾酒分页列表")
    @GetMapping("/cocktails")
    public ApiResponse<Page<AdminCocktailListResponse>> cocktails(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminService.listCocktails(keyword, category, page, size));
    }

    @Operation(summary = "鸡尾酒分类列表")
    @GetMapping("/cocktails/categories")
    public ApiResponse<List<String>> cocktailCategories() {
        return ApiResponse.success(adminService.listCocktailCategories());
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

    @Operation(summary = "新增规范化鸡尾酒")
    @PostMapping("/cocktails/generated")
    public ApiResponse<AdminCocktailDetailResponse> createGeneratedCocktail(
            @Valid @RequestBody AdminGeneratedCocktailSaveRequest request) {
        return ApiResponse.success(adminService.createGeneratedCocktail(request));
    }

    @Operation(summary = "修改鸡尾酒")
    @PutMapping("/cocktails/{id}")
    public ApiResponse<AdminCocktailDetailResponse> updateCocktail(
            @PathVariable Long id,
            @Valid @RequestBody AdminCocktailSaveRequest request) {
        return ApiResponse.success(adminService.updateCocktail(id, request));
    }

    @Operation(summary = "修改规范化鸡尾酒")
    @PutMapping("/cocktails/generated/{id}")
    public ApiResponse<AdminCocktailDetailResponse> updateGeneratedCocktail(
            @PathVariable Long id,
            @Valid @RequestBody AdminGeneratedCocktailSaveRequest request) {
        return ApiResponse.success(adminService.updateGeneratedCocktail(id, request));
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

    @Operation(summary = "抓取网页HTML原文")
    @PostMapping("/crawl/page-text")
    public ApiResponse<AdminPageTextResponse> crawlPageText(@Valid @RequestBody AdminPageCrawlRequest request) {
        return ApiResponse.success(adminPageCrawlService.crawlPageText(request.getUrl()));
    }

    @Operation(summary = "从HTML原文提取配方字段")
    @PostMapping("/crawl/extract-fields")
    public ApiResponse<AdminPageExtractFieldsResponse> extractFields(@Valid @RequestBody AdminPageFieldExtractRequest request) {
        return ApiResponse.success(adminPageExtractService.extractFields(request));
    }

    @Operation(summary = "基于已提取字段生成中文最终结果")
    @PostMapping("/crawl/generate-fields")
    public ApiResponse<AdminPageExtractFieldsResponse> generateFields(@Valid @RequestBody AdminPageAiGenerateRequest request) {
        return ApiResponse.success(adminPageAiGenerateService.generateChineseFields(request.toExtractedResponse()));
    }

    @Operation(summary = "从列表页批量抓取详情并提取字段")
    @PostMapping("/crawl/import-from-list")
    public ApiResponse<AdminPageBatchImportResponse> importFromList(@Valid @RequestBody AdminPageBatchImportRequest request) {
        return ApiResponse.success(adminPageBatchImportService.importFromList(request));
    }

    @Operation(summary = "创建批量抓取任务（异步）")
    @PostMapping("/crawl/import-from-list/jobs")
    public ApiResponse<AdminBatchImportJobStartResponse> startImportJob(@Valid @RequestBody AdminPageBatchImportRequest request) {
        return ApiResponse.success(adminPageBatchImportService.startImportJob(request));
    }

    @Operation(summary = "查询批量抓取任务状态")
    @GetMapping("/crawl/import-from-list/jobs/{jobId}")
    public ApiResponse<AdminBatchImportJobStatusResponse> importJobStatus(@PathVariable String jobId) {
        return ApiResponse.success(adminPageBatchImportService.getImportJobStatus(jobId));
    }

    @Operation(summary = "批量抓取执行历史")
    @GetMapping("/crawl/import-histories")
    public ApiResponse<AdminPageResult<AdminBatchImportHistoryResponse>> importHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminPageBatchImportService.listImportHistories(page, size));
    }
}
