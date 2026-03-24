package com.shakepro.dto.response;

import com.shakepro.entity.Cocktail;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CocktailListResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private String description;
    private Integer alcoholLevel;

    public static CocktailListResponse from(Cocktail cocktail) {
        return CocktailListResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .imageUrl(cocktail.getImageUrl())
                .description(cocktail.getDescription())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .build();
    }
}
