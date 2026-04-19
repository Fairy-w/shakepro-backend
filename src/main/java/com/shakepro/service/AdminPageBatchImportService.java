package com.shakepro.service;

import com.shakepro.dto.request.admin.AdminPageBatchImportRequest;
import com.shakepro.dto.response.admin.AdminBatchImportHistoryResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStartResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStatusResponse;
import com.shakepro.dto.response.admin.AdminPageResult;
import com.shakepro.dto.response.admin.AdminPageBatchImportResponse;

public interface AdminPageBatchImportService {

    AdminPageBatchImportResponse importFromList(AdminPageBatchImportRequest request);

    AdminPageResult<AdminBatchImportHistoryResponse> listImportHistories(int page, int size);

    AdminBatchImportJobStartResponse startImportJob(AdminPageBatchImportRequest request);

    AdminBatchImportJobStatusResponse getImportJobStatus(String jobId);
}
