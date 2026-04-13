package com.shakepro.service;

import com.shakepro.dto.request.recipe.RecipeDetailPageUpdateRequest;
import com.shakepro.dto.request.recipe.RecipeReviewPublishRequest;
import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import com.shakepro.dto.response.recipe.RecipeReviewPublishResponse;

import java.util.List;

public interface RecipePublishService {

    RecipeReviewPublishResponse getCandidateDetail(Long detailContentId);

    List<RecipeReviewPublishResponse> listCandidateDetails(String status);

    RecipeReviewPublishResponse saveCandidateDetail(Long detailContentId, RecipeDetailPageUpdateRequest request);

    RecipeReviewPublishResponse reviewAndPublish(RecipeReviewPublishRequest request);

    RecipeDetailPageResponse getPublishedDetail(String recipeKey);

    List<RecipeDetailPageResponse> listPublishedDetails();
}
