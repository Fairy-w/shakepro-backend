package com.shakepro.dto.response.admin;

import com.shakepro.entity.CrawlBatchImportHistory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminBatchImportHistoryResponse {

    private Long id;
    private String listUrl;
    private String listTitle;
    private Boolean onlyNew;
    private Integer maxItems;
    private Integer concurrency;
    private Boolean autoGenerate;
    private Boolean autoSave;
    private Integer discoveredCount;
    private Integer selectedCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer remainingUnimportedCount;
    private Long durationMs;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static AdminBatchImportHistoryResponse from(CrawlBatchImportHistory history) {
        return AdminBatchImportHistoryResponse.builder()
                .id(history.getId())
                .listUrl(history.getListUrl())
                .listTitle(history.getListTitle())
                .onlyNew(history.getOnlyNew())
                .maxItems(history.getMaxItems())
                .concurrency(history.getConcurrency())
                .autoGenerate(history.getAutoGenerate())
                .autoSave(history.getAutoSave())
                .discoveredCount(history.getDiscoveredCount())
                .selectedCount(history.getSelectedCount())
                .processedCount(history.getProcessedCount())
                .successCount(history.getSuccessCount())
                .failureCount(history.getFailureCount())
                .remainingUnimportedCount(history.getRemainingUnimportedCount())
                .durationMs(history.getDurationMs())
                .status(history.getStatus())
                .errorMessage(history.getErrorMessage())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
