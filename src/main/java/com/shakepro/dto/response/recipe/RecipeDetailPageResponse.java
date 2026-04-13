package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeDetailPageResponse {

    private String id;
    private String name;
    private String englishName;
    private String category;
    private String heroImage;
    private String highlight;
    private String subtitle;
    private String description;
    private String story;
    private String bestFor;
    private String difficulty;
    private String duration;
    private String abv;
    private String volume;
    private String glass;
    private String garnish;
    private String serveTemperature;
    private List<String> flavorTags;
    private List<FlavorMetricItemResponse> flavorMetrics;
    private List<String> pairings;
    private List<String> serviceNotes;
    private List<IngredientItemResponse> ingredients;
    private List<StepItemResponse> steps;

    @Data
    @Builder
    public static class FlavorMetricItemResponse {
        private String label;
        private Integer value;
    }

    @Data
    @Builder
    public static class IngredientItemResponse {
        private String name;
        private String amount;
        private String note;
    }

    @Data
    @Builder
    public static class StepItemResponse {
        private String title;
        private String detail;
        private String hint;
    }
}
