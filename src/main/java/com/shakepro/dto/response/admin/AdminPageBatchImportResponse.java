package com.shakepro.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageBatchImportResponse {

    private String listUrl;
    private String listTitle;
    private int discoveredCount;
    private int selectedCount;
    private int processedCount;
    private int successCount;
    private int failureCount;
    private int remainingUnimportedCount;
    private long durationMs;
    @Builder.Default
    private List<ImportItemResult> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportItemResult {
        private int index;
        private String url;
        private String status;
        private String stage;
        private String title;
        private String name;
        private Long savedCocktailId;
        private String errorMessage;
        @Builder.Default
        private List<String> missingFields = new ArrayList<>();
        private AdminPageExtractFieldsResponse fields;
    }
}
