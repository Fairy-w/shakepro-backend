package com.shakepro.dto.response.admin;

import com.shakepro.entity.Cocktail;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminCocktailListResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private Integer alcoholLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminCocktailListResponse from(Cocktail cocktail) {
        return AdminCocktailListResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .imageUrl(cocktail.getImageUrl())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .createdAt(cocktail.getCreatedAt())
                .updatedAt(cocktail.getUpdatedAt())
                .build();
    }
}
