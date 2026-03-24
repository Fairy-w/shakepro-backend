package com.shakepro.service;

import com.shakepro.dto.request.FileRecordRequest;
import com.shakepro.dto.request.PresignRequest;
import com.shakepro.dto.response.PresignResponse;

public interface OssService {

    PresignResponse generatePresignUrl(PresignRequest request, Long userId);

    Long saveFileRecord(FileRecordRequest request, Long userId);
}
