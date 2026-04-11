package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCocktailFavoriteStatusResponse {

    private boolean favorited;
    private Long favoriteId;
}
