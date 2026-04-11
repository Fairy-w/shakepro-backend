package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminAiCocktailFavoriteResponse {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String recipeKey;
    private String name;
    private String description;
    private List<String> materials;
    private List<String> steps;
    private String prompt;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
