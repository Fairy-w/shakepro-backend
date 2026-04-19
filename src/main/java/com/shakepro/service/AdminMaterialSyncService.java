package com.shakepro.service;

import com.shakepro.dto.request.admin.AdminMaterialSyncRequest;
import com.shakepro.dto.response.admin.AdminMaterialSyncResponse;

public interface AdminMaterialSyncService {

    AdminMaterialSyncResponse syncFromCocktailDb(AdminMaterialSyncRequest request);
}
