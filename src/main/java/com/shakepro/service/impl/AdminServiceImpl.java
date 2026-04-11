package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.JwtUtil;
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
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CocktailMaterial;
import com.shakepro.entity.FavoriteAiCocktail;
import com.shakepro.entity.Material;
import com.shakepro.entity.User;
import com.shakepro.entity.UserRole;
import com.shakepro.repository.CocktailMaterialRepository;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.FavoriteAiCocktailRepository;
import com.shakepro.repository.FavoriteRepository;
import com.shakepro.repository.FileRecordRepository;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CocktailRepository cocktailRepository;
    private final CocktailMaterialRepository cocktailMaterialRepository;
    private final MaterialRepository materialRepository;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteAiCocktailRepository favoriteAiCocktailRepository;
    private final FileRecordRepository fileRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public AdminLoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        log.info("Admin logged in: id={}, username={}", user.getId(), user.getUsername());

        return AdminLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expireSeconds(jwtUtil.getExpirationSeconds())
                .user(AdminProfileResponse.from(user))
                .build();
    }

    @Override
    public AdminProfileResponse getCurrentAdmin(Long userId) {
        User user = getAdminUser(userId);
        return AdminProfileResponse.from(user);
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        long aiFavoriteCount = favoriteAiCocktailRepository.count();
        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalAdmins(userRepository.countByRole(UserRole.ADMIN))
                .totalCocktails(cocktailRepository.count())
                .totalMaterials(materialRepository.count())
                .totalFavorites(favoriteRepository.count() + aiFavoriteCount)
                .totalAiCocktailFavorites(aiFavoriteCount)
                .totalFiles(fileRecordRepository.count())
                .build();
    }

    @Override
    public Page<AdminUserResponse> listUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            users = userRepository.findByUsernameContainingIgnoreCaseOrNicknameContainingIgnoreCase(trimmed, trimmed, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(AdminUserResponse::from);
    }

    @Override
    public Page<AdminAiCocktailFavoriteResponse> listAiCocktailFavorites(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase();
        Page<FavoriteAiCocktail> favorites = favoriteAiCocktailRepository.searchForAdmin(normalizedKeyword, pageable);
        Map<Long, User> userMap = userRepository.findAllById(favorites.getContent().stream()
                        .map(FavoriteAiCocktail::getUserId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return favorites.map(favorite -> toAdminAiCocktailFavoriteResponse(favorite, userMap.get(favorite.getUserId())));
    }

    @Override
    public List<AdminMaterialResponse> listMaterials(String keyword, String category) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase();

        return materialRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(material -> normalizedKeyword.isBlank()
                        || material.getName().toLowerCase().contains(normalizedKeyword))
                .filter(material -> normalizedCategory.isBlank()
                        || (material.getCategory() != null && material.getCategory().toLowerCase().contains(normalizedCategory)))
                .map(AdminMaterialResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AdminMaterialResponse createMaterial(AdminMaterialSaveRequest request) {
        String name = request.getName().trim();
        if (materialRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "材料名称已存在");
        }

        Material material = Material.builder()
                .name(name)
                .category(normalizeCategory(request.getCategory()))
                .build();
        return AdminMaterialResponse.from(materialRepository.save(material));
    }

    @Override
    @Transactional
    public AdminMaterialResponse updateMaterial(Long id, AdminMaterialSaveRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "材料不存在"));

        String name = request.getName().trim();
        if (!material.getName().equalsIgnoreCase(name) && materialRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "材料名称已存在");
        }

        material.setName(name);
        material.setCategory(normalizeCategory(request.getCategory()));
        return AdminMaterialResponse.from(materialRepository.save(material));
    }

    @Override
    @Transactional
    public void deleteMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "材料不存在"));
        if (cocktailMaterialRepository.existsByMaterialId(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该材料已被鸡尾酒配方使用，不能删除");
        }
        materialRepository.delete(material);
    }

    @Override
    public Page<AdminCocktailListResponse> listCocktails(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Cocktail> cocktails;
        if (keyword != null && !keyword.isBlank()) {
            cocktails = cocktailRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            cocktails = cocktailRepository.findAll(pageable);
        }
        return cocktails.map(AdminCocktailListResponse::from);
    }

    @Override
    public AdminCocktailDetailResponse getCocktail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        return AdminCocktailDetailResponse.from(cocktail, cocktailMaterialRepository.findByCocktailId(id));
    }

    @Override
    @Transactional
    public AdminCocktailDetailResponse createCocktail(AdminCocktailSaveRequest request) {
        Cocktail cocktail = Cocktail.builder().build();
        applyCocktailFields(cocktail, request);
        Cocktail saved = cocktailRepository.save(cocktail);
        replaceMaterials(saved, request.getMaterials());
        return AdminCocktailDetailResponse.from(saved, cocktailMaterialRepository.findByCocktailId(saved.getId()));
    }

    @Override
    @Transactional
    public AdminCocktailDetailResponse updateCocktail(Long id, AdminCocktailSaveRequest request) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        applyCocktailFields(cocktail, request);
        Cocktail saved = cocktailRepository.save(cocktail);
        cocktailMaterialRepository.deleteByCocktailId(id);
        replaceMaterials(saved, request.getMaterials());
        return AdminCocktailDetailResponse.from(saved, cocktailMaterialRepository.findByCocktailId(saved.getId()));
    }

    @Override
    @Transactional
    public void deleteCocktail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        cocktailMaterialRepository.deleteByCocktailId(id);
        cocktailRepository.delete(cocktail);
    }

    @Override
    @Transactional
    public void deleteAiCocktailFavorite(Long id) {
        FavoriteAiCocktail favorite = favoriteAiCocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AI配方收藏不存在"));
        favoriteAiCocktailRepository.delete(favorite);
    }

    private User getAdminUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        return user;
    }

    private void applyCocktailFields(Cocktail cocktail, AdminCocktailSaveRequest request) {
        cocktail.setName(request.getName().trim());
        cocktail.setDescription(trimToNull(request.getDescription()));
        cocktail.setImageUrl(trimToNull(request.getImageUrl()));
        cocktail.setAlcoholLevel(request.getAlcoholLevel());
        cocktail.setSteps(trimToNull(request.getSteps()));
    }

    private void replaceMaterials(Cocktail cocktail, List<AdminCocktailSaveRequest.MaterialItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<Long> materialIds = requests.stream()
                .map(AdminCocktailSaveRequest.MaterialItemRequest::getMaterialId)
                .distinct()
                .toList();

        Map<Long, Material> materialMap = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, material -> material, (left, right) -> left, HashMap::new));

        if (materialMap.size() != materialIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "材料信息不完整或包含不存在的材料");
        }

        List<CocktailMaterial> cocktailMaterials = requests.stream()
                .map(item -> CocktailMaterial.builder()
                        .cocktail(cocktail)
                        .material(materialMap.get(item.getMaterialId()))
                        .amount(item.getAmount().trim())
                        .build())
                .toList();

        cocktailMaterialRepository.saveAll(cocktailMaterials);
    }

    private AdminAiCocktailFavoriteResponse toAdminAiCocktailFavoriteResponse(FavoriteAiCocktail favorite, User user) {
        return AdminAiCocktailFavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUserId())
                .username(user != null ? user.getUsername() : null)
                .nickname(user != null ? user.getNickname() : null)
                .recipeKey(favorite.getRecipeKey())
                .name(favorite.getName())
                .description(favorite.getDescription())
                .materials(readStringList(favorite.getMaterialsJson()))
                .steps(readStringList(favorite.getStepsJson()))
                .prompt(favorite.getPrompt())
                .source(favorite.getSource())
                .createdAt(favorite.getCreatedAt())
                .updatedAt(favorite.getUpdatedAt())
                .build();
    }

    private String normalizeCategory(String category) {
        return trimToNull(category);
    }

    private List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "解析AI配方收藏数据失败");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
