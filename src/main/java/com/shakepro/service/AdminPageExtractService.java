package com.shakepro.service;

import com.shakepro.dto.request.admin.AdminPageFieldExtractRequest;
import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;

public interface AdminPageExtractService {

    AdminPageExtractFieldsResponse extractFields(AdminPageFieldExtractRequest request);
}
