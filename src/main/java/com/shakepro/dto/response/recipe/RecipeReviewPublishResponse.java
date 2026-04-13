package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipeReviewPublishResponse {

    private Long detailContentId;
    private String recipeKey;
    private String action;
    private String status;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;
    private RecipeDetailValidationResult validation;
    private RecipeDetailPageResponse detail;
}
