package com.shakepro.service;

import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;

public interface AdminPageAiGenerateService {

    AdminPageExtractFieldsResponse generateChineseFields(AdminPageExtractFieldsResponse extracted);
}
