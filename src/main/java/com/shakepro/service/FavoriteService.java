package com.shakepro.service;

import com.shakepro.dto.response.CocktailListResponse;

import java.util.List;

public interface FavoriteService {

    void addFavorite(Long userId, Long cocktailId);

    void removeFavorite(Long userId, Long cocktailId);

    List<CocktailListResponse> listFavorites(Long userId);
}
