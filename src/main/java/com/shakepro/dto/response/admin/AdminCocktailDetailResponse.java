package com.shakepro.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminCocktailDetailResponse {

    private Long id;
    private String name;
    private String englishName;
    private String category;
    private String heroImage;
    private String difficulty;
    private String abv;
    private String glass;
    private String garnish;
    private String highlight;
    private String subtitle;
    private String description;
    private String story;
    private String imageUrl;
    private Integer alcoholLevel;
    private String legacySteps;
    private List<String> flavorTags;
    private List<FlavorMetricItemResponse> flavorMetrics;
    private List<String> pairings;
    private List<String> serviceNotes;
    private List<StepItemResponse> steps;
    private List<MaterialItemResponse> materials;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialItemResponse {
        private Long materialId;
        private String name;
        private String category;
        private String displayName;
        private String amount;
        private String note;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepItemResponse {
        private Integer order;
        private String title;
        private String detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlavorMetricItemResponse {
        private Integer sortOrder;
        private String name;
        private Integer value;
    }
}
