package com.shakepro.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AiCocktailFavoriteItemResponse {

    private Long favoriteId;
    private String recipeKey;
    private String name;
    private String description;
    private List<String> materials;
    private List<String> steps;
    private String prompt;
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
