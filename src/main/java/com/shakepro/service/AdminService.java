package com.shakepro.service;

import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.request.admin.AdminCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminMaterialSaveRequest;
import com.shakepro.dto.response.admin.AdminAiCocktailFavoriteResponse;
import com.shakepro.dto.response.admin.AdminCocktailDetailResponse;
import com.shakepro.dto.response.admin.AdminCocktailListResponse;
import com.shakepro.dto.response.admin.AdminDashboardResponse;
import com.shakepro.dto.response.admin.AdminLoginResponse;
import com.shakepro.dto.response.admin.AdminMaterialResponse;
import com.shakepro.dto.response.admin.AdminProfileResponse;
import com.shakepro.dto.response.admin.AdminUserResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminService {

    AdminLoginResponse login(LoginRequest request);

    AdminProfileResponse getCurrentAdmin(Long userId);

    AdminDashboardResponse getDashboard();

    Page<AdminUserResponse> listUsers(String keyword, int page, int size);

    Page<AdminAiCocktailFavoriteResponse> listAiCocktailFavorites(String keyword, int page, int size);

    List<AdminMaterialResponse> listMaterials(String keyword, String category);

    AdminMaterialResponse createMaterial(AdminMaterialSaveRequest request);

    AdminMaterialResponse updateMaterial(Long id, AdminMaterialSaveRequest request);

    void deleteMaterial(Long id);

    Page<AdminCocktailListResponse> listCocktails(String keyword, int page, int size);

    AdminCocktailDetailResponse getCocktail(Long id);

    AdminCocktailDetailResponse createCocktail(AdminCocktailSaveRequest request);

    AdminCocktailDetailResponse updateCocktail(Long id, AdminCocktailSaveRequest request);

    void deleteCocktail(Long id);

    void deleteAiCocktailFavorite(Long id);
}
