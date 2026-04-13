package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.RecipePipelineStatuses;
import com.shakepro.dto.request.recipe.RecipeDetailPageUpdateRequest;
import com.shakepro.dto.request.recipe.RecipeReviewPublishRequest;
import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import com.shakepro.dto.response.recipe.RecipeDetailValidationResult;
import com.shakepro.dto.response.recipe.RecipeReviewPublishResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CocktailMaterial;
import com.shakepro.entity.Material;
import com.shakepro.entity.RecipeDetailContent;
import com.shakepro.entity.RecipeSourceRecord;
import com.shakepro.entity.RecipeStructuredRecord;
import com.shakepro.repository.CocktailMaterialRepository;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.repository.RecipeDetailContentRepository;
import com.shakepro.repository.RecipeSourceRecordRepository;
import com.shakepro.repository.RecipeStructuredRecordRepository;
import com.shakepro.service.RecipePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecipePublishServiceImpl implements RecipePublishService {

    private final RecipeDetailContentRepository recipeDetailContentRepository;
    private final RecipeStructuredRecordRepository recipeStructuredRecordRepository;
    private final RecipeSourceRecordRepository recipeSourceRecordRepository;
    private final CocktailRepository cocktailRepository;
    private final CocktailMaterialRepository cocktailMaterialRepository;
    private final MaterialRepository materialRepository;
    private final ObjectMapper objectMapper;
    private final RecipeDetailValidator recipeDetailValidator;

    @Override
    public RecipeReviewPublishResponse getCandidateDetail(Long detailContentId) {
        RecipeDetailContent content = recipeDetailContentRepository.findById(detailContentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "候选详情不存在"));
        RecipeDetailPageResponse detail = toDetailPageResponse(content);
        return buildReviewResponse(content, "查询候选", null, detail, recipeDetailValidator.validate(detail));
    }

    @Override
    public List<RecipeReviewPublishResponse> listCandidateDetails(String status) {
        List<RecipeDetailContent> records;
        if (status == null || status.isBlank()) {
            records = recipeDetailContentRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        } else {
            records = recipeDetailContentRepository.findByStatusOrderByUpdatedAtDesc(status);
        }
        return records.stream()
                .map(content -> {
                    RecipeDetailPageResponse detail = toDetailPageResponse(content);
                    return buildReviewResponse(content, "候选列表", null, detail, recipeDetailValidator.validate(detail));
                })
                .toList();
    }

    @Override
    @Transactional
    public RecipeReviewPublishResponse saveCandidateDetail(Long detailContentId, RecipeDetailPageUpdateRequest request) {
        RecipeDetailContent content = recipeDetailContentRepository.findById(detailContentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "候选详情不存在"));
        applyUpdateRequest(content, request);
        content.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
        content.setReviewedAt(LocalDateTime.now());
        RecipeDetailContent saved = recipeDetailContentRepository.save(content);
        RecipeDetailPageResponse detail = toDetailPageResponse(saved);
        RecipeDetailValidationResult validation = recipeDetailValidator.validate(detail);
        return buildReviewResponse(saved, "保存草稿", "已保存到候选层", detail, validation);
    }

    @Override
    @Transactional
    public RecipeReviewPublishResponse reviewAndPublish(RecipeReviewPublishRequest request) {
        RecipeDetailContent content = recipeDetailContentRepository.findById(request.getDetailContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "待审核详情不存在"));
        RecipeStructuredRecord structuredRecord = content.getStructuredRecord();
        RecipeSourceRecord sourceRecord = structuredRecord.getSourceRecord();

        if (request.getDetail() != null) {
            applyUpdateRequest(content, request.getDetail());
        }

        String action = request.getAction().trim();
        LocalDateTime now = LocalDateTime.now();
        content.setReviewedAt(now);
        RecipeDetailPageResponse detail = toDetailPageResponse(content);
        RecipeDetailValidationResult validation = recipeDetailValidator.validate(detail);

        if (action.contains("驳回")) {
            content.setStatus(RecipePipelineStatuses.REJECTED);
            structuredRecord.setStatus(RecipePipelineStatuses.REJECTED);
            sourceRecord.setStatus(RecipePipelineStatuses.REJECTED);
            content.setPublishedAt(null);
        } else if (Boolean.TRUE.equals(request.getPublishNow()) || action.contains("发布") || action.contains("通过")) {
            if (!validation.isPassed()) {
                content.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
                structuredRecord.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
                sourceRecord.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
                content.setPublishedAt(null);
            } else {
                syncToFormalCocktail(detail);
                content.setStatus(RecipePipelineStatuses.PUBLISHED);
                structuredRecord.setStatus(RecipePipelineStatuses.PUBLISHED);
                sourceRecord.setStatus(RecipePipelineStatuses.PUBLISHED);
                content.setPublishedAt(now);
            }
        } else {
            content.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
            structuredRecord.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
            sourceRecord.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
        }

        recipeSourceRecordRepository.save(sourceRecord);
        recipeStructuredRecordRepository.save(structuredRecord);
        RecipeDetailContent saved = recipeDetailContentRepository.save(content);

        return buildReviewResponse(saved, action, request.getReviewComment(), detail, validation);
    }

    @Override
    public RecipeDetailPageResponse getPublishedDetail(String recipeKey) {
        RecipeDetailContent content = recipeDetailContentRepository.findByRecipeKey(recipeKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "详情内容不存在"));
        if (!RecipePipelineStatuses.PUBLISHED.equals(content.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该详情尚未发布");
        }
        return toDetailPageResponse(content);
    }

    @Override
    public List<RecipeDetailPageResponse> listPublishedDetails() {
        return recipeDetailContentRepository.findByStatusOrderByUpdatedAtDesc(RecipePipelineStatuses.PUBLISHED).stream()
                .map(this::toDetailPageResponse)
                .toList();
    }

    private void syncToFormalCocktail(RecipeDetailPageResponse detail) {
        Cocktail cocktail = cocktailRepository.findByNameIgnoreCase(detail.getName())
                .orElseGet(Cocktail::new);
        cocktail.setName(detail.getName());
        cocktail.setDescription(detail.getDescription());
        cocktail.setImageUrl(detail.getHeroImage());
        cocktail.setAlcoholLevel(parseAlcoholLevel(detail.getAbv()));
        cocktail.setSteps(buildCocktailSteps(detail));
        Cocktail savedCocktail = cocktailRepository.save(cocktail);

        cocktailMaterialRepository.deleteByCocktailId(savedCocktail.getId());
        List<CocktailMaterial> cocktailMaterials = new ArrayList<>();
        for (RecipeDetailPageResponse.IngredientItemResponse ingredient : detail.getIngredients()) {
            Material material = materialRepository.findByNameIgnoreCase(ingredient.getName())
                    .orElseGet(() -> materialRepository.save(Material.builder()
                            .name(ingredient.getName())
                            .category(detectMaterialCategory(ingredient.getName()))
                            .build()));
            cocktailMaterials.add(CocktailMaterial.builder()
                    .cocktail(savedCocktail)
                    .material(material)
                    .amount(ingredient.getAmount())
                    .build());
        }
        cocktailMaterialRepository.saveAll(cocktailMaterials);
    }

    private Integer parseAlcoholLevel(String abv) {
        if (abv == null || abv.isBlank()) {
            return null;
        }
        String digits = abv.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private String buildCocktailSteps(RecipeDetailPageResponse detail) {
        List<String> lines = new ArrayList<>();
        int index = 1;
        for (RecipeDetailPageResponse.StepItemResponse step : detail.getSteps()) {
            lines.add(index + ". " + step.getTitle() + "：" + step.getDetail());
            index++;
        }
        return String.join("\n", lines);
    }

    private String detectMaterialCategory(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("gin") || lower.contains("rum") || lower.contains("vodka")
                || lower.contains("tequila") || lower.contains("whisky") || lower.contains("whiskey")
                || lower.contains("brandy") || lower.contains("金酒") || lower.contains("朗姆")
                || lower.contains("伏特加") || lower.contains("龙舌兰") || lower.contains("威士忌")) {
            return "spirit";
        }
        if (lower.contains("juice") || lower.contains("汁")) {
            return "juice";
        }
        if (lower.contains("syrup") || lower.contains("糖浆")) {
            return "syrup";
        }
        if (lower.contains("peel") || lower.contains("olive") || lower.contains("mint")
                || lower.contains("柠檬皮") || lower.contains("橄榄") || lower.contains("薄荷")) {
            return "garnish";
        }
        return "other";
    }

    private void applyUpdateRequest(RecipeDetailContent content, RecipeDetailPageUpdateRequest detail) {
        content.setRecipeKey(detail.getId());
        content.setName(detail.getName());
        content.setEnglishName(detail.getEnglishName());
        content.setCategory(detail.getCategory());
        content.setHeroImage(detail.getHeroImage());
        content.setHighlight(detail.getHighlight());
        content.setSubtitle(detail.getSubtitle());
        content.setDescription(detail.getDescription());
        content.setStory(detail.getStory());
        content.setBestFor(detail.getBestFor());
        content.setDifficulty(detail.getDifficulty());
        content.setDuration(detail.getDuration());
        content.setAbv(detail.getAbv());
        content.setVolume(detail.getVolume());
        content.setGlass(detail.getGlass());
        content.setGarnish(detail.getGarnish());
        content.setServeTemperature(detail.getServeTemperature());
        content.setFlavorTagsJson(writeJson(detail.getFlavorTags()));
        content.setFlavorMetricsJson(writeJson(detail.getFlavorMetrics()));
        content.setPairingsJson(writeJson(detail.getPairings()));
        content.setServiceNotesJson(writeJson(detail.getServiceNotes()));
        content.setIngredientsJson(writeJson(detail.getIngredients()));
        content.setStepsJson(writeJson(detail.getSteps()));
    }

    private RecipeReviewPublishResponse buildReviewResponse(
            RecipeDetailContent content,
            String action,
            String reviewComment,
            RecipeDetailPageResponse detail,
            RecipeDetailValidationResult validation
    ) {
        return RecipeReviewPublishResponse.builder()
                .detailContentId(content.getId())
                .recipeKey(content.getRecipeKey())
                .action(action)
                .status(content.getStatus())
                .reviewComment(reviewComment)
                .reviewedAt(content.getReviewedAt())
                .publishedAt(content.getPublishedAt())
                .validation(validation)
                .detail(detail)
                .build();
    }

    private RecipeDetailPageResponse toDetailPageResponse(RecipeDetailContent content) {
        return RecipeDetailPageResponse.builder()
                .id(content.getRecipeKey())
                .name(content.getName())
                .englishName(content.getEnglishName())
                .category(content.getCategory())
                .heroImage(content.getHeroImage())
                .highlight(content.getHighlight())
                .subtitle(content.getSubtitle())
                .description(content.getDescription())
                .story(content.getStory())
                .bestFor(content.getBestFor())
                .difficulty(content.getDifficulty())
                .duration(content.getDuration())
                .abv(content.getAbv())
                .volume(content.getVolume())
                .glass(content.getGlass())
                .garnish(content.getGarnish())
                .serveTemperature(content.getServeTemperature())
                .flavorTags(readList(content.getFlavorTagsJson(), new TypeReference<List<String>>() {}))
                .flavorMetrics(readList(content.getFlavorMetricsJson(), new TypeReference<List<RecipeDetailPageResponse.FlavorMetricItemResponse>>() {}))
                .pairings(readList(content.getPairingsJson(), new TypeReference<List<String>>() {}))
                .serviceNotes(readList(content.getServiceNotesJson(), new TypeReference<List<String>>() {}))
                .ingredients(readList(content.getIngredientsJson(), new TypeReference<List<RecipeDetailPageResponse.IngredientItemResponse>>() {}))
                .steps(readList(content.getStepsJson(), new TypeReference<List<RecipeDetailPageResponse.StepItemResponse>>() {}))
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "详情内容序列化失败");
        }
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "详情内容解析失败");
        }
    }
}
