package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBatchImportJobStatusResponse {

    private String jobId;
    private String status;
    private String message;
    private String listUrl;
    private String listTitle;
    private Integer maxItems;
    private Integer concurrency;
    private Boolean autoGenerate;
    private Boolean autoSave;
    private Boolean onlyNew;
    private Integer discoveredCount;
    private Integer selectedCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer remainingUnimportedCount;
    private Integer progressPercent;
    private Long durationMs;
    private String currentUrl;
    private String currentStage;
    private Long startedAtEpochMs;
    private Long updatedAtEpochMs;
    private String errorMessage;
}
