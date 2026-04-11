package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardResponse {

    private long totalUsers;
    private long totalAdmins;
    private long totalCocktails;
    private long totalMaterials;
    private long totalFavorites;
    private long totalAiCocktailFavorites;
    private long totalFiles;
}
