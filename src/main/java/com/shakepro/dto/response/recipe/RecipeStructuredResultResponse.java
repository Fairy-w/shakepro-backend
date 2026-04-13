package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RecipeStructuredResultResponse {

    private Long id;
    private Long sourceRecordId;
    private String recipeKey;
    private String englishName;
    private String chineseNameDraft;
    private String category;
    private String heroImage;
    private String garnish;
    private String glassDraft;
    private String methodText;
    private String estimatedAbv;
    private String estimatedVolume;
    private List<IngredientItemResponse> ingredients;
    private List<StepItemResponse> steps;
    private String parseNotes;
    private String status;
    private LocalDateTime parsedAt;
    private String sourceSite;
    private String sourceUrl;

    @Data
    @Builder
    public static class IngredientItemResponse {
        private String name;
        private String amount;
        private String note;
        private String category;
    }

    @Data
    @Builder
    public static class StepItemResponse {
        private Integer orderNo;
        private String title;
        private String detail;
        private String hint;
    }
}
