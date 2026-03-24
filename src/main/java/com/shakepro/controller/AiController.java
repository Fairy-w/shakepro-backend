package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.dto.request.AiRecommendRequest;
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
}
