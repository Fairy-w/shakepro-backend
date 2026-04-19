package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBatchImportJobStartResponse {

    private String jobId;
    private String status;
    private String message;
}
