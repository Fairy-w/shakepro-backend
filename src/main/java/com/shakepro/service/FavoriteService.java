package com.shakepro.service;

import com.shakepro.dto.request.AiCocktailFavoriteCreateRequest;
import com.shakepro.dto.response.AiCocktailFavoriteActionResponse;
import com.shakepro.dto.response.AiCocktailFavoritePageResponse;
import com.shakepro.dto.response.AiCocktailFavoriteStatusResponse;
import com.shakepro.dto.response.CocktailListResponse;

import java.util.List;

public interface FavoriteService {

    void addFavorite(Long userId, Long cocktailId);

    void removeFavorite(Long userId, Long cocktailId);

    List<CocktailListResponse> listFavorites(Long userId);

    AiCocktailFavoriteActionResponse addAiCocktailFavorite(Long userId, AiCocktailFavoriteCreateRequest request);

    AiCocktailFavoriteActionResponse removeAiCocktailFavorite(Long userId, Long favoriteId);

    AiCocktailFavoriteActionResponse removeAiCocktailFavoriteByRecipeKey(Long userId, String recipeKey);

    AiCocktailFavoritePageResponse listAiCocktailFavorites(Long userId, String keyword, int pageNo, int pageSize, String sort);

    AiCocktailFavoriteStatusResponse getAiCocktailFavoriteStatus(Long userId, String recipeKey);
}
