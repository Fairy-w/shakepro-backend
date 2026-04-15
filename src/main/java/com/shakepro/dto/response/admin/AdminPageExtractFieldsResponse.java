package com.shakepro.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageExtractFieldsResponse {

    private String url;
    private String title;
    private String extractMode;
    private String generateMode;
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
    @Builder.Default
    private List<String> flavorTags = new ArrayList<>();
    @Builder.Default
    private Map<String, Integer> flavorMetrics = new LinkedHashMap<>();
    @Builder.Default
    private List<String> pairings = new ArrayList<>();
    @Builder.Default
    private List<String> serviceNotes = new ArrayList<>();
    @Builder.Default
    private List<IngredientItem> ingredients = new ArrayList<>();
    @Builder.Default
    private List<StepItem> steps = new ArrayList<>();
    @Builder.Default
    private Map<String, FieldSource> fieldSources = new LinkedHashMap<>();
    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientItem {
        private String name;
        private String amount;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepItem {
        private String title;
        private String detail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldSource {
        private String mode;
        private String source;
        private String note;
    }
}
