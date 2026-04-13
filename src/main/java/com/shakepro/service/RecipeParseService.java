package com.shakepro.service;

import com.shakepro.dto.response.recipe.RecipeStructuredResultResponse;

import java.util.List;

public interface RecipeParseService {

    RecipeStructuredResultResponse parseSourceRecord(Long sourceRecordId);

    RecipeStructuredResultResponse getStructuredRecord(Long structuredRecordId);

    List<RecipeStructuredResultResponse> listStructuredRecordsByStatus(String status);
}
