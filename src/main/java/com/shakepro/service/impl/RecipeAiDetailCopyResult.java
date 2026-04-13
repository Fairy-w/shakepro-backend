package com.shakepro.service.impl;

import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeAiDetailCopyResult {

    private String name;
    private String highlight;
    private String subtitle;
    private String description;
    private String story;
    private String bestFor;
    private List<String> flavorTags;
    private List<RecipeDetailPageResponse.FlavorMetricItemResponse> flavorMetrics;
    private List<String> pairings;
    private List<String> serviceNotes;
}
