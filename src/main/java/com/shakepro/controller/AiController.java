package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.request.AiGenerateRecipeByTextRequest;
import com.shakepro.dto.request.AiGenerateRecipeRequest;
import com.shakepro.dto.request.AiRecommendRequest;
import com.shakepro.dto.response.AiGenerateRecipeResponse;
import com.shakepro.dto.response.AiRecommendResponse;
import com.shakepro.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI", description = "AI推荐相关接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI推荐鸡尾酒")
    @PostMapping("/recommend")
    public ApiResponse<List<AiRecommendResponse>> recommend(@Valid @RequestBody AiRecommendRequest request) {
        return ApiResponse.success(aiService.recommend(request));
    }

    @Operation(summary = "AI生成调酒配方（通义千问）")
    @PostMapping("/generate-recipe")
    public ApiResponse<List<AiGenerateRecipeResponse>> generateRecipe(@Valid @RequestBody AiGenerateRecipeRequest request) {
        return ApiResponse.success(aiService.generateRecipe(request));
    }

    @Operation(summary = "AI自然语言生成调酒配方（通义千问）")
    @PostMapping("/generate-recipe-by-text")
    public ApiResponse<List<AiGenerateRecipeResponse>> generateRecipeByText(
            @Valid @RequestBody AiGenerateRecipeByTextRequest request) {
        return ApiResponse.success(aiService.generateRecipeByText(request));
    }
}
