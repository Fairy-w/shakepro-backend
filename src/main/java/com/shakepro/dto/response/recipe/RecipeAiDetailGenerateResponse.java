package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipeAiDetailGenerateResponse {

    private Long detailContentId;
    private Long structuredRecordId;
    private String recipeKey;
    private String status;
    private LocalDateTime aiGeneratedAt;
    private String sourceSite;
    private String sourceUrl;
    private RecipeDetailValidationResult validation;
    private RecipeDetailPageResponse detail;
}
