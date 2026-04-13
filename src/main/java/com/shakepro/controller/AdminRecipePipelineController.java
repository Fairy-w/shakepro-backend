package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.request.recipe.RecipeCrawlTaskRequest;
import com.shakepro.dto.request.recipe.RecipeDetailPageUpdateRequest;
import com.shakepro.dto.request.recipe.RecipeReviewPublishRequest;
import com.shakepro.dto.response.recipe.RecipeAiDetailGenerateResponse;
import com.shakepro.dto.response.recipe.RecipeCrawlTaskResponse;
import com.shakepro.dto.response.recipe.RecipeReviewPublishResponse;
import com.shakepro.dto.response.recipe.RecipeSourceRecordResponse;
import com.shakepro.dto.response.recipe.RecipeStructuredResultResponse;
import com.shakepro.service.CrawlTaskService;
import com.shakepro.service.RecipeAiGenerateService;
import com.shakepro.service.RecipeParseService;
import com.shakepro.service.RecipePublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Recipe Pipeline", description = "配方采集、解析、AI 生成、审核发布接口")
@RestController
@RequestMapping("/api/admin/recipe-pipeline")
@RequiredArgsConstructor
public class AdminRecipePipelineController {

    private final CrawlTaskService crawlTaskService;
    private final RecipeParseService recipeParseService;
    private final RecipeAiGenerateService recipeAiGenerateService;
    private final RecipePublishService recipePublishService;

    @Operation(summary = "创建抓取任务")
    @PostMapping("/crawl-tasks")
    public ApiResponse<RecipeCrawlTaskResponse> createCrawlTask(@Valid @RequestBody RecipeCrawlTaskRequest request) {
        return ApiResponse.success(crawlTaskService.crawl(request));
    }

    @Operation(summary = "查询抓取任务列表（按原始记录展示）")
    @GetMapping({"/crawl-tasks", "/source-records"})
    public ApiResponse<List<RecipeSourceRecordResponse>> sourceRecords(@RequestParam(required = false) String status) {
        return ApiResponse.success(crawlTaskService.listSourceRecordsByStatus(status));
    }

    @Operation(summary = "查看原始页面内容")
    @GetMapping("/source-records/{id}")
    public ApiResponse<RecipeSourceRecordResponse> sourceRecord(@PathVariable Long id) {
        return ApiResponse.success(crawlTaskService.getSourceRecord(id));
    }

    @Operation(summary = "抓取审核通过后执行 AI 结构化解析")
    @PostMapping("/source-records/{id}/parse")
    public ApiResponse<RecipeStructuredResultResponse> parse(@PathVariable Long id) {
        return ApiResponse.success(recipeParseService.parseSourceRecord(id));
    }

    @Operation(summary = "驳回原始抓取记录")
    @PostMapping("/source-records/{id}/reject")
    public ApiResponse<RecipeSourceRecordResponse> rejectSource(@PathVariable Long id) {
        return ApiResponse.success(crawlTaskService.rejectSourceRecord(id));
    }

    @Operation(summary = "查询结构化结果列表")
    @GetMapping("/structured-records")
    public ApiResponse<List<RecipeStructuredResultResponse>> structuredRecords(@RequestParam(required = false) String status) {
        return ApiResponse.success(recipeParseService.listStructuredRecordsByStatus(status));
    }

    @Operation(summary = "查看结构化结果")
    @GetMapping("/structured-records/{id}")
    public ApiResponse<RecipeStructuredResultResponse> structuredRecord(@PathVariable Long id) {
        return ApiResponse.success(recipeParseService.getStructuredRecord(id));
    }

    @Operation(summary = "执行 AI 详情生成")
    @PostMapping("/structured-records/{id}/ai-generate")
    public ApiResponse<RecipeAiDetailGenerateResponse> generateDetail(@PathVariable Long id) {
        return ApiResponse.success(recipeAiGenerateService.generateDetail(id));
    }

    @Operation(summary = "查询候选详情列表")
    @GetMapping("/candidates")
    public ApiResponse<List<RecipeReviewPublishResponse>> candidateDetails(@RequestParam(required = false) String status) {
        return ApiResponse.success(recipePublishService.listCandidateDetails(status));
    }

    @Operation(summary = "查询候选详情")
    @GetMapping("/candidates/{id}")
    public ApiResponse<RecipeReviewPublishResponse> candidateDetail(@PathVariable Long id) {
        return ApiResponse.success(recipePublishService.getCandidateDetail(id));
    }

    @Operation(summary = "保存审核修改")
    @PutMapping("/candidates/{id}")
    public ApiResponse<RecipeReviewPublishResponse> saveCandidateDetail(
            @PathVariable Long id,
            @Valid @RequestBody RecipeDetailPageUpdateRequest request
    ) {
        return ApiResponse.success(recipePublishService.saveCandidateDetail(id, request));
    }

    @Operation(summary = "发布配方")
    @PostMapping("/candidates/{id}/publish")
    public ApiResponse<RecipeReviewPublishResponse> publish(
            @PathVariable Long id,
            @RequestBody(required = false) RecipeReviewPublishRequest request
    ) {
        RecipeReviewPublishRequest actualRequest = request == null ? new RecipeReviewPublishRequest() : request;
        actualRequest.setDetailContentId(id);
        actualRequest.setAction("发布");
        actualRequest.setPublishNow(true);
        return ApiResponse.success(recipePublishService.reviewAndPublish(actualRequest));
    }

    @Operation(summary = "驳回配方")
    @PostMapping("/candidates/{id}/reject")
    public ApiResponse<RecipeReviewPublishResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RecipeReviewPublishRequest request
    ) {
        RecipeReviewPublishRequest actualRequest = request == null ? new RecipeReviewPublishRequest() : request;
        actualRequest.setDetailContentId(id);
        actualRequest.setAction("驳回");
        actualRequest.setPublishNow(false);
        return ApiResponse.success(recipePublishService.reviewAndPublish(actualRequest));
    }
}
