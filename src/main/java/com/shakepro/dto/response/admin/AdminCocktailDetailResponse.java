package com.shakepro.dto.response.admin;

import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CocktailMaterial;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminCocktailDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer alcoholLevel;
    private String steps;
    private List<MaterialItemResponse> materials;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminCocktailDetailResponse from(Cocktail cocktail, List<CocktailMaterial> materials) {
        return AdminCocktailDetailResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .description(cocktail.getDescription())
                .imageUrl(cocktail.getImageUrl())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .steps(cocktail.getSteps())
                .materials(materials.stream()
                        .map(material -> MaterialItemResponse.builder()
                                .materialId(material.getMaterial().getId())
                                .name(material.getMaterial().getName())
                                .category(material.getMaterial().getCategory())
                                .amount(material.getAmount())
                                .build())
                        .toList())
                .createdAt(cocktail.getCreatedAt())
                .updatedAt(cocktail.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    public static class MaterialItemResponse {
        private Long materialId;
        private String name;
        private String category;
        private String amount;
    }
}
