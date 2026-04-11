package com.shakepro.service;

import com.shakepro.dto.request.AiRecommendRequest;
import com.shakepro.dto.request.AiGenerateRecipeRequest;
import com.shakepro.dto.request.AiGenerateRecipeByTextRequest;
import com.shakepro.dto.response.AiGenerateRecipeResponse;
import com.shakepro.dto.response.AiRecommendResponse;

import java.util.List;

public interface AiService {

    List<AiRecommendResponse> recommend(AiRecommendRequest request);

    List<AiGenerateRecipeResponse> generateRecipe(AiGenerateRecipeRequest request);

    List<AiGenerateRecipeResponse> generateRecipeByText(AiGenerateRecipeByTextRequest request);
}
