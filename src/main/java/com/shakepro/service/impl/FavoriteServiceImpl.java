package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.request.AiCocktailFavoriteCreateRequest;
import com.shakepro.dto.response.AiCocktailFavoriteActionResponse;
import com.shakepro.dto.response.AiCocktailFavoriteItemResponse;
import com.shakepro.dto.response.AiCocktailFavoritePageResponse;
import com.shakepro.dto.response.AiCocktailFavoriteStatusResponse;
import com.shakepro.dto.response.CocktailListResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.Favorite;
import com.shakepro.entity.FavoriteAiCocktail;
import com.shakepro.entity.User;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.FavoriteAiCocktailRepository;
import com.shakepro.repository.FavoriteRepository;
import com.shakepro.repository.UserRepository;
import com.shakepro.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CocktailRepository cocktailRepository;
    private final FavoriteAiCocktailRepository favoriteAiCocktailRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void addFavorite(Long userId, Long cocktailId) {
        if (favoriteRepository.existsByUserIdAndCocktailId(userId, cocktailId)) {
            return; // Already favorited, idempotent
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "鸡尾酒不存在"));

        Favorite favorite = Favorite.builder()
                .user(user)
                .cocktail(cocktail)
                .build();
        favoriteRepository.save(favorite);
        log.info("User {} favorited cocktail {}", userId, cocktailId);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long cocktailId) {
        Favorite favorite = favoriteRepository.findByUserIdAndCocktailId(userId, cocktailId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在"));
        favoriteRepository.delete(favorite);
        log.info("User {} unfavorited cocktail {}", userId, cocktailId);
    }

    @Override
    public List<CocktailListResponse> listFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(f -> CocktailListResponse.from(f.getCocktail()))
                .toList();
    }

    @Override
    @Transactional
    public AiCocktailFavoriteActionResponse addAiCocktailFavorite(Long userId, AiCocktailFavoriteCreateRequest request) {
        String recipeKey = resolveRecipeKey(request);
        FavoriteAiCocktail existing = favoriteAiCocktailRepository.findByUserIdAndRecipeKey(userId, recipeKey).orElse(null);
        if (existing != null) {
            return buildActionResponse(existing.getId(), true);
        }

        FavoriteAiCocktail favorite = FavoriteAiCocktail.builder()
                .userId(userId)
                .recipeKey(recipeKey)
                .name(normalizeText(request.getName()))
                .description(normalizeNullableText(request.getDescription()))
                .materialsJson(writeStringList(normalizeStringList(request.getMaterials())))
                .stepsJson(writeStringList(normalizeStringList(request.getSteps())))
                .prompt(normalizeNullableText(request.getPrompt()))
                .source(resolveSource(request.getSource()))
                .build();

        try {
            FavoriteAiCocktail saved = favoriteAiCocktailRepository.save(favorite);
            log.info("User {} favorited AI cocktail recipe {}", userId, recipeKey);
            return buildActionResponse(saved.getId(), true);
        } catch (DataIntegrityViolationException e) {
            FavoriteAiCocktail duplicated = favoriteAiCocktailRepository.findByUserIdAndRecipeKey(userId, recipeKey)
                    .orElseThrow(() -> e);
            return buildActionResponse(duplicated.getId(), true);
        }
    }

    @Override
    @Transactional
    public AiCocktailFavoriteActionResponse removeAiCocktailFavorite(Long userId, Long favoriteId) {
        FavoriteAiCocktail favorite = favoriteAiCocktailRepository.findByIdAndUserId(favoriteId, userId).orElse(null);
        if (favorite == null) {
            return buildActionResponse(favoriteId, false);
        }

        favoriteAiCocktailRepository.delete(favorite);
        log.info("User {} unfavorited AI cocktail favoriteId {}", userId, favoriteId);
        return buildActionResponse(favoriteId, false);
    }

    @Override
    @Transactional
    public AiCocktailFavoriteActionResponse removeAiCocktailFavoriteByRecipeKey(Long userId, String recipeKey) {
        FavoriteAiCocktail favorite = favoriteAiCocktailRepository.findByUserIdAndRecipeKey(userId, recipeKey.trim()).orElse(null);
        if (favorite == null) {
            return buildActionResponse(null, false);
        }

        favoriteAiCocktailRepository.delete(favorite);
        log.info("User {} unfavorited AI cocktail recipe {}", userId, favorite.getRecipeKey());
        return buildActionResponse(favorite.getId(), false);
    }

    @Override
    public AiCocktailFavoritePageResponse listAiCocktailFavorites(Long userId, String keyword, int pageNo, int pageSize, String sort) {
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, buildAiFavoriteSort(sort));
        Page<FavoriteAiCocktail> page = favoriteAiCocktailRepository.search(userId, normalizeKeyword(keyword), pageable);
        List<AiCocktailFavoriteItemResponse> list = page.getContent().stream()
                .map(this::toAiCocktailFavoriteItemResponse)
                .toList();
        return AiCocktailFavoritePageResponse.builder()
                .total(page.getTotalElements())
                .list(list)
                .build();
    }

    @Override
    public AiCocktailFavoriteStatusResponse getAiCocktailFavoriteStatus(Long userId, String recipeKey) {
        FavoriteAiCocktail favorite = favoriteAiCocktailRepository.findByUserIdAndRecipeKey(userId, recipeKey.trim()).orElse(null);
        return AiCocktailFavoriteStatusResponse.builder()
                .favorited(favorite != null)
                .favoriteId(favorite != null ? favorite.getId() : null)
                .build();
    }

    private AiCocktailFavoriteActionResponse buildActionResponse(Long favoriteId, boolean favorited) {
        return AiCocktailFavoriteActionResponse.builder()
                .favoriteId(favoriteId)
                .favorited(favorited)
                .build();
    }

    private AiCocktailFavoriteItemResponse toAiCocktailFavoriteItemResponse(FavoriteAiCocktail favorite) {
        return AiCocktailFavoriteItemResponse.builder()
                .favoriteId(favorite.getId())
                .recipeKey(favorite.getRecipeKey())
                .name(favorite.getName())
                .description(favorite.getDescription())
                .materials(readStringList(favorite.getMaterialsJson()))
                .steps(readStringList(favorite.getStepsJson()))
                .prompt(favorite.getPrompt())
                .source(favorite.getSource())
                .createdAt(favorite.getCreatedAt())
                .build();
    }

    private Sort buildAiFavoriteSort(String sortValue) {
        if (sortValue == null || sortValue.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sortValue.split(",", 2);
        String property = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "desc";

        if (!List.of("createdAt", "updatedAt", "name").contains(property)) {
            property = "createdAt";
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, property);
    }

    private String resolveRecipeKey(AiCocktailFavoriteCreateRequest request) {
        if (request.getRecipeKey() != null && !request.getRecipeKey().isBlank()) {
            return request.getRecipeKey().trim();
        }

        List<String> materials = normalizeStringList(request.getMaterials());
        List<String> steps = normalizeStringList(request.getSteps());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", normalizeText(request.getName()));
        payload.put("description", normalizeNullableText(request.getDescription()));
        payload.put("materials", materials);
        payload.put("steps", steps);

        try {
            String canonicalPayload = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "生成recipeKey失败");
        }
    }

    private String resolveSource(String source) {
        String normalized = normalizeNullableText(source);
        return normalized == null ? "ai" : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeKeyword(String keyword) {
        String normalized = normalizeNullableText(keyword);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> normalizeStringList(List<String> values) {
        return values.stream()
                .map(String::trim)
                .toList();
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "序列化AI配方字段失败");
        }
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "反序列化AI配方字段失败");
        }
    }
}
