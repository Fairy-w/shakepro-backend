package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.JwtUtil;
import com.shakepro.common.util.OssImageUrlBuilder;
import com.shakepro.config.OssConfig;
import com.shakepro.dto.request.LoginRequest;
import com.shakepro.dto.request.admin.AdminCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminGeneratedCocktailSaveRequest;
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
import com.shakepro.entity.CocktailFlavorMetric;
import com.shakepro.entity.CocktailFlavorTag;
import com.shakepro.entity.CocktailMaterial;
import com.shakepro.entity.CocktailPairing;
import com.shakepro.entity.CocktailServiceNote;
import com.shakepro.entity.CocktailStep;
import com.shakepro.entity.FavoriteAiCocktail;
import com.shakepro.entity.FileRecord;
import com.shakepro.entity.Material;
import com.shakepro.entity.User;
import com.shakepro.entity.UserRole;
import com.shakepro.repository.CocktailFlavorMetricRepository;
import com.shakepro.repository.CocktailFlavorTagRepository;
import com.shakepro.repository.CocktailMaterialRepository;
import com.shakepro.repository.CocktailPairingRepository;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.CocktailServiceNoteRepository;
import com.shakepro.repository.CocktailStepRepository;
import com.shakepro.repository.FavoriteAiCocktailRepository;
import com.shakepro.repository.FavoriteRepository;
import com.shakepro.repository.FileRecordRepository;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CocktailRepository cocktailRepository;
    private final CocktailMaterialRepository cocktailMaterialRepository;
    private final CocktailStepRepository cocktailStepRepository;
    private final CocktailFlavorTagRepository cocktailFlavorTagRepository;
    private final CocktailFlavorMetricRepository cocktailFlavorMetricRepository;
    private final CocktailPairingRepository cocktailPairingRepository;
    private final CocktailServiceNoteRepository cocktailServiceNoteRepository;
    private final MaterialRepository materialRepository;
    private final FavoriteRepository favoriteRepository;
    private final FavoriteAiCocktailRepository favoriteAiCocktailRepository;
    private final FileRecordRepository fileRecordRepository;
    private final OSS ossClient;
    private final OssConfig ossConfig;
    private final OssImageUrlBuilder ossImageUrlBuilder;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final long MAX_AUTO_UPLOAD_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> BLOCKED_IMAGE_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");
    private static final Map<String, String> CONTENT_TYPE_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "image/svg+xml", ".svg",
            "image/avif", ".avif"
    );

    private final HttpClient imageFetchHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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
                .map(this::toAdminMaterialResponse)
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
        return toAdminMaterialResponse(materialRepository.save(material));
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
        return toAdminMaterialResponse(materialRepository.save(material));
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
    public Page<AdminCocktailListResponse> listCocktails(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Cocktail> cocktails;
        String normalizedKeyword = keyword != null ? keyword.trim() : null;
        String normalizedCategory = category != null ? category.trim() : null;

        if (normalizedKeyword != null && !normalizedKeyword.isBlank() && normalizedCategory != null && !normalizedCategory.isBlank()) {
            cocktails = cocktailRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(normalizedKeyword, normalizedCategory, pageable);
        } else if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
            cocktails = cocktailRepository.findByNameContainingIgnoreCase(normalizedKeyword, pageable);
        } else if (normalizedCategory != null && !normalizedCategory.isBlank()) {
            cocktails = cocktailRepository.findByCategoryIgnoreCase(normalizedCategory, pageable);
        } else {
            cocktails = cocktailRepository.findAll(pageable);
        }
        return cocktails.map(AdminCocktailListResponse::from);
    }

    @Override
    public List<String> listCocktailCategories() {
        return cocktailRepository.findDistinctCategories();
    }

    @Override
    public AdminCocktailDetailResponse getCocktail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        return buildAdminCocktailDetailResponse(cocktail);
    }

    @Override
    @Transactional
    public AdminCocktailDetailResponse createCocktail(AdminCocktailSaveRequest request) {
        Cocktail cocktail = Cocktail.builder().build();
        applyCocktailFields(cocktail, request);
        Cocktail saved = cocktailRepository.save(cocktail);
        replaceMaterials(saved, request.getMaterials());
        return buildAdminCocktailDetailResponse(saved);
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
        return buildAdminCocktailDetailResponse(saved);
    }

    @Override
    @Transactional
    public AdminCocktailDetailResponse createGeneratedCocktail(AdminGeneratedCocktailSaveRequest request) {
        Cocktail cocktail = Cocktail.builder().build();
        applyGeneratedCocktailFields(cocktail, request);
        Cocktail saved;
        try {
            saved = cocktailRepository.saveAndFlush(cocktail);
        } catch (DataIntegrityViolationException ex) {
            String sourceUrl = trimToNull(request.getSourceUrl());
            if (sourceUrl == null) {
                throw ex;
            }
            Cocktail duplicated = cocktailRepository.findBySourceUrl(sourceUrl).orElseThrow(() -> ex);
            log.info("Generated cocktail skipped due to duplicated sourceUrl: {}", sourceUrl);
            return buildAdminCocktailDetailResponse(duplicated);
        }
        replaceGeneratedMaterials(saved, request.getIngredients());
        replaceCocktailSteps(saved, request.getSteps());
        replaceCocktailFlavorTags(saved, request.getFlavorTags());
        replaceCocktailFlavorMetrics(saved, request.getFlavorMetrics());
        replaceCocktailPairings(saved, request.getPairings());
        replaceCocktailServiceNotes(saved, request.getServiceNotes());
        return buildAdminCocktailDetailResponse(saved);
    }

    @Override
    @Transactional
    public AdminCocktailDetailResponse updateGeneratedCocktail(Long id, AdminGeneratedCocktailSaveRequest request) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
        applyGeneratedCocktailFields(cocktail, request);
        Cocktail saved = cocktailRepository.save(cocktail);
        cocktailMaterialRepository.deleteByCocktailId(id);
        cocktailStepRepository.deleteByCocktailId(id);
        cocktailFlavorTagRepository.deleteByCocktailId(id);
        cocktailFlavorMetricRepository.deleteByCocktailId(id);
        cocktailPairingRepository.deleteByCocktailId(id);
        cocktailServiceNoteRepository.deleteByCocktailId(id);
        replaceGeneratedMaterials(saved, request.getIngredients());
        replaceCocktailSteps(saved, request.getSteps());
        replaceCocktailFlavorTags(saved, request.getFlavorTags());
        replaceCocktailFlavorMetrics(saved, request.getFlavorMetrics());
        replaceCocktailPairings(saved, request.getPairings());
        replaceCocktailServiceNotes(saved, request.getServiceNotes());
        return buildAdminCocktailDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCocktail(Long id) {
        Cocktail cocktail = cocktailRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));
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

    private void applyGeneratedCocktailFields(Cocktail cocktail, AdminGeneratedCocktailSaveRequest request) {
        cocktail.setName(request.getName().trim());
        cocktail.setEnglishName(trimToNull(request.getEnglishName()));
        cocktail.setCategory(trimToNull(request.getCategory()));
        cocktail.setHeroImage(resolveHeroImageForStorage(trimToNull(request.getHeroImage())));
        cocktail.setDifficulty(trimToNull(request.getDifficulty()));
        cocktail.setAbv(trimToNull(request.getAbv()));
        cocktail.setGlass(trimToNull(request.getGlass()));
        cocktail.setGarnish(trimToNull(request.getGarnish()));
        cocktail.setHighlight(trimToNull(request.getHighlight()));
        cocktail.setSubtitle(trimToNull(request.getSubtitle()));
        cocktail.setDescription(trimToNull(request.getDescription()));
        cocktail.setStory(trimToNull(request.getStory()));
        cocktail.setSourceUrl(trimToNull(request.getSourceUrl()));
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
                        .displayName(materialMap.get(item.getMaterialId()).getName())
                        .amount(item.getAmount().trim())
                        .sortOrder(0)
                        .build())
                .toList();

        cocktailMaterialRepository.saveAll(cocktailMaterials);
    }

    private void replaceGeneratedMaterials(Cocktail cocktail, List<AdminGeneratedCocktailSaveRequest.IngredientItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<Long> materialIds = requests.stream()
                .map(AdminGeneratedCocktailSaveRequest.IngredientItemRequest::getMaterialId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, Material> materialMap = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, material -> material, (left, right) -> left, LinkedHashMap::new));
        if (materialMap.size() != materialIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在不存在的材料ID");
        }

        List<CocktailMaterial> cocktailMaterials = requests.stream()
                .map(item -> {
                    Material material = item.getMaterialId() == null ? null : materialMap.get(item.getMaterialId());
                    return CocktailMaterial.builder()
                            .cocktail(cocktail)
                            .material(material)
                            .displayName(trimToNull(item.getName()))
                            .amount(trimToNull(item.getAmount()))
                            .note(trimToNull(item.getNote()))
                            .sortOrder(requests.indexOf(item))
                            .build();
                })
                .toList();
        cocktailMaterialRepository.saveAll(cocktailMaterials);
    }

    private void replaceCocktailSteps(Cocktail cocktail, List<AdminGeneratedCocktailSaveRequest.StepItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<CocktailStep> stepItems = new java.util.ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AdminGeneratedCocktailSaveRequest.StepItemRequest item = requests.get(i);
            stepItems.add(CocktailStep.builder()
                    .cocktail(cocktail)
                    .stepOrder(i + 1)
                    .title(trimToNull(item.getTitle()))
                    .detail(item.getDetail().trim())
                    .build());
        }
        cocktailStepRepository.saveAll(stepItems);
    }

    private void replaceCocktailFlavorTags(Cocktail cocktail, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<CocktailFlavorTag> entities = new java.util.ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            String tag = trimToNull(tags.get(i));
            if (tag == null) {
                continue;
            }
            entities.add(CocktailFlavorTag.builder()
                    .cocktail(cocktail)
                    .tag(tag)
                    .sortOrder(i)
                    .build());
        }
        cocktailFlavorTagRepository.saveAll(entities);
    }

    private void replaceCocktailFlavorMetrics(Cocktail cocktail, List<AdminGeneratedCocktailSaveRequest.FlavorMetricItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<CocktailFlavorMetric> entities = new java.util.ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            AdminGeneratedCocktailSaveRequest.FlavorMetricItemRequest item = requests.get(i);
            entities.add(CocktailFlavorMetric.builder()
                    .cocktail(cocktail)
                    .metricName(item.getName().trim())
                    .metricValue(item.getValue())
                    .sortOrder(i)
                    .build());
        }
        cocktailFlavorMetricRepository.saveAll(entities);
    }

    private void replaceCocktailPairings(Cocktail cocktail, List<String> pairings) {
        if (pairings == null || pairings.isEmpty()) {
            return;
        }
        List<CocktailPairing> entities = new java.util.ArrayList<>();
        for (int i = 0; i < pairings.size(); i++) {
            String pairing = trimToNull(pairings.get(i));
            if (pairing == null) {
                continue;
            }
            entities.add(CocktailPairing.builder()
                    .cocktail(cocktail)
                    .pairing(pairing)
                    .sortOrder(i)
                    .build());
        }
        cocktailPairingRepository.saveAll(entities);
    }

    private void replaceCocktailServiceNotes(Cocktail cocktail, List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        List<CocktailServiceNote> entities = new java.util.ArrayList<>();
        for (int i = 0; i < notes.size(); i++) {
            String note = trimToNull(notes.get(i));
            if (note == null) {
                continue;
            }
            entities.add(CocktailServiceNote.builder()
                    .cocktail(cocktail)
                    .note(note)
                    .sortOrder(i)
                    .build());
        }
        cocktailServiceNoteRepository.saveAll(entities);
    }

    private AdminCocktailDetailResponse buildAdminCocktailDetailResponse(Cocktail cocktail) {
        List<CocktailMaterial> materials = cocktailMaterialRepository.findByCocktailIdOrderBySortOrderAscIdAsc(cocktail.getId());
        List<CocktailStep> steps = cocktailStepRepository.findByCocktailIdOrderByStepOrderAsc(cocktail.getId());
        List<CocktailFlavorTag> flavorTags = cocktailFlavorTagRepository.findByCocktailIdOrderBySortOrderAsc(cocktail.getId());
        List<CocktailFlavorMetric> flavorMetrics = cocktailFlavorMetricRepository.findByCocktailIdOrderBySortOrderAsc(cocktail.getId());
        List<CocktailPairing> pairings = cocktailPairingRepository.findByCocktailIdOrderBySortOrderAsc(cocktail.getId());
        List<CocktailServiceNote> serviceNotes = cocktailServiceNoteRepository.findByCocktailIdOrderBySortOrderAsc(cocktail.getId());

        return AdminCocktailDetailResponse.builder()
                .id(cocktail.getId())
                .name(cocktail.getName())
                .englishName(cocktail.getEnglishName())
                .category(cocktail.getCategory())
                .heroImage(cocktail.getHeroImage())
                .difficulty(cocktail.getDifficulty())
                .abv(cocktail.getAbv())
                .glass(cocktail.getGlass())
                .garnish(cocktail.getGarnish())
                .highlight(cocktail.getHighlight())
                .subtitle(cocktail.getSubtitle())
                .description(cocktail.getDescription())
                .story(cocktail.getStory())
                .imageUrl(cocktail.getImageUrl())
                .alcoholLevel(cocktail.getAlcoholLevel())
                .legacySteps(cocktail.getSteps())
                .flavorTags(flavorTags.stream().map(CocktailFlavorTag::getTag).toList())
                .flavorMetrics(flavorMetrics.stream()
                        .map(metric -> AdminCocktailDetailResponse.FlavorMetricItemResponse.builder()
                                .sortOrder(metric.getSortOrder())
                                .name(metric.getMetricName())
                                .value(metric.getMetricValue())
                                .build())
                        .toList())
                .pairings(pairings.stream().map(CocktailPairing::getPairing).toList())
                .serviceNotes(serviceNotes.stream().map(CocktailServiceNote::getNote).toList())
                .steps(steps.stream()
                        .map(step -> AdminCocktailDetailResponse.StepItemResponse.builder()
                                .order(step.getStepOrder())
                                .title(step.getTitle())
                                .detail(step.getDetail())
                                .build())
                        .toList())
                .materials(materials.stream()
                        .map(material -> AdminCocktailDetailResponse.MaterialItemResponse.builder()
                                .materialId(material.getMaterial() != null ? material.getMaterial().getId() : null)
                                .name(material.getMaterial() != null ? material.getMaterial().getName() : material.getDisplayName())
                                .category(material.getMaterial() != null ? material.getMaterial().getCategory() : null)
                                .displayName(material.getDisplayName())
                                .amount(material.getAmount())
                                .note(material.getNote())
                                .sortOrder(material.getSortOrder())
                                .build())
                        .toList())
                .createdAt(cocktail.getCreatedAt())
                .updatedAt(cocktail.getUpdatedAt())
                .build();
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

    private AdminMaterialResponse toAdminMaterialResponse(Material material) {
        AdminMaterialResponse response = AdminMaterialResponse.from(material);
        response.setImageUrlThumb(ossImageUrlBuilder.toThumbUrl(response.getImageUrl()));
        response.setImageUrlCard(ossImageUrlBuilder.toCardUrl(response.getImageUrl()));
        response.setImageUrlDetail(ossImageUrlBuilder.toDetailUrl(response.getImageUrl()));
        return response;
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

    private String resolveHeroImageForStorage(String heroImageUrl) {
        if (heroImageUrl == null || isAlreadyManagedOssUrl(heroImageUrl)) {
            return heroImageUrl;
        }
        try {
            UploadedImage uploaded = uploadRemoteImageToOss(heroImageUrl);
            fileRecordRepository.save(FileRecord.builder()
                    .userId(null)
                    .objectKey(uploaded.objectKey())
                    .url(uploaded.publicUrl())
                    .contentType(uploaded.contentType())
                    .size(uploaded.size())
                    .build());
            return uploaded.publicUrl();
        } catch (Exception ex) {
            log.warn("Failed to mirror heroImage to OSS, fallback to original url: {}, reason={}", heroImageUrl, ex.getMessage());
            return heroImageUrl;
        }
    }

    private boolean isAlreadyManagedOssUrl(String url) {
        String publicBaseUrl = trimToNull(ossConfig.getNormalizedPublicBaseUrl());
        return publicBaseUrl != null && url.startsWith(publicBaseUrl + "/");
    }

    private UploadedImage uploadRemoteImageToOss(String imageUrl) throws Exception {
        URI uri = URI.create(imageUrl);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("heroImage 仅支持 http/https 链接");
        }
        String host = uri.getHost();
        if (host == null || BLOCKED_IMAGE_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("heroImage host 不允许");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "image/*")
                .GET()
                .build();

        HttpResponse<InputStream> response = imageFetchHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("下载图片失败，状态码=" + statusCode);
        }

        String contentType = normalizeContentType(response.headers().firstValue("Content-Type").orElse(""));
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("heroImage 响应不是图片类型");
        }

        long contentLength = parseContentLength(response.headers().firstValue("Content-Length").orElse(null));
        if (contentLength > MAX_AUTO_UPLOAD_IMAGE_SIZE) {
            throw new IllegalArgumentException("heroImage 超过 10MB 限制");
        }

        byte[] imageBytes;
        try (InputStream body = response.body()) {
            imageBytes = readImageBytes(body);
        }

        String extension = detectExtension(uri, contentType);
        String objectKey = "uploads/generated/" + UUID.randomUUID().toString().replace("-", "") + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(imageBytes.length);
        ossClient.putObject(
                ossConfig.getNormalizedBucket(),
                objectKey,
                new ByteArrayInputStream(imageBytes),
                metadata
        );

        String publicUrl = ossConfig.getNormalizedPublicBaseUrl() + "/" + objectKey;
        return new UploadedImage(objectKey, publicUrl, contentType, (long) imageBytes.length);
    }

    private long parseContentLength(String contentLength) {
        if (contentLength == null || contentLength.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(contentLength.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private byte[] readImageBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        long total = 0L;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_AUTO_UPLOAD_IMAGE_SIZE) {
                throw new IllegalArgumentException("heroImage 超过 10MB 限制");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String normalizeContentType(String rawContentType) {
        int separator = rawContentType.indexOf(';');
        String normalized = separator >= 0 ? rawContentType.substring(0, separator) : rawContentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String detectExtension(URI uri, String contentType) {
        String path = uri.getPath() == null ? "" : uri.getPath();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < path.length() - 1) {
            String ext = path.substring(dotIndex);
            if (ext.length() <= 8) {
                return ext.toLowerCase(Locale.ROOT);
            }
        }
        return CONTENT_TYPE_EXTENSION.getOrDefault(contentType, ".img");
    }

    private record UploadedImage(String objectKey, String publicUrl, String contentType, Long size) {
    }
}
