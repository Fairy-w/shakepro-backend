package com.shakepro.service;

import com.shakepro.dto.response.UserMaterialResponse;

import java.util.List;

public interface AdminUserMaterialManageService {

    List<UserMaterialResponse> list(Long userId, String keyword, String categoryId);
}
