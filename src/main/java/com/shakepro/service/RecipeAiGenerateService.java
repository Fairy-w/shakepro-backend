package com.shakepro.service;

import com.shakepro.dto.response.recipe.RecipeAiDetailGenerateResponse;

import java.util.List;

public interface RecipeAiGenerateService {

    RecipeAiDetailGenerateResponse generateDetail(Long structuredRecordId);

    RecipeAiDetailGenerateResponse getDetailGeneration(Long detailContentId);

    List<RecipeAiDetailGenerateResponse> listDetailsByStatus(String status);
}
