package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CocktailDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer alcoholLevel;
    private String steps;
    private List<MaterialItemResponse> materials;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class MaterialItemResponse {
        private Long materialId;
        private String name;
        private String category;
        private String amount;
    }
}
