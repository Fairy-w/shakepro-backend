package com.shakepro.dto.response;

import com.shakepro.entity.Cocktail;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CocktailListResponse {

    private Long id;
    private String name;
    private String englishName;
    private String category;
    private String heroImage;
    private String difficulty;
    private String abv;
    private String imageUrl;
    private String imageUrlThumb;
    private String imageUrlCard;
    private String description;
    private Integer alcoholLevel;

    public static CocktailListResponse from(Cocktail cocktail) {
        String preferredImageUrl = cocktail.getHeroImage() != null ? cocktail.getHeroImage() : cocktail.getImageUrl();
        return CocktailListResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .englishName(cocktail.getEnglishName())
                .category(cocktail.getCategory())
                .heroImage(cocktail.getHeroImage())
                .difficulty(cocktail.getDifficulty())
                .abv(cocktail.getAbv())
                .imageUrl(preferredImageUrl)
                .imageUrlThumb(preferredImageUrl)
                .imageUrlCard(preferredImageUrl)
                .description(cocktail.getDescription())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .build();
    }
}
