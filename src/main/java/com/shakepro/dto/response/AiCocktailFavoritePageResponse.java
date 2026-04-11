package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiCocktailFavoritePageResponse {

    private long total;
    private List<AiCocktailFavoriteItemResponse> list;
}
