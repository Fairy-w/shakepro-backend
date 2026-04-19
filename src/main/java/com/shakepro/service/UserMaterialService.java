package com.shakepro.service;

import com.shakepro.dto.request.UserMaterialManualSaveRequest;
import com.shakepro.dto.request.UserMaterialSaveRequest;
import com.shakepro.dto.response.UserMaterialResponse;

import java.util.List;

public interface UserMaterialService {

    UserMaterialResponse saveFromScan(Long userId, UserMaterialSaveRequest request);

    UserMaterialResponse saveManual(Long userId, UserMaterialManualSaveRequest request);

    List<UserMaterialResponse> saveManualBatch(Long userId, List<UserMaterialManualSaveRequest> requests);

    List<UserMaterialResponse> list(Long userId, String keyword, String categoryId);

    void removeByBarcode(Long userId, String barcode);
}
