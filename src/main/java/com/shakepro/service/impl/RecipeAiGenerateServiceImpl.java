package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.RecipePipelineStatuses;
import com.shakepro.config.AiConfig;
import com.shakepro.config.AiQwenConfig;
import com.shakepro.dto.response.recipe.RecipeAiDetailGenerateResponse;
import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import com.shakepro.dto.response.recipe.RecipeDetailValidationResult;
import com.shakepro.dto.response.recipe.RecipeStructuredResultResponse;
import com.shakepro.entity.RecipeDetailContent;
import com.shakepro.entity.RecipeSourceRecord;
import com.shakepro.entity.RecipeStructuredRecord;
import com.shakepro.repository.RecipeDetailContentRepository;
import com.shakepro.repository.RecipeSourceRecordRepository;
import com.shakepro.repository.RecipeStructuredRecordRepository;
import com.shakepro.service.RecipeAiGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeAiGenerateServiceImpl implements RecipeAiGenerateService {

    private final AiConfig aiConfig;
    private final AiQwenConfig aiQwenConfig;
    private final HttpClient aiHttpClient;
    private final ObjectMapper objectMapper;
    private final RecipeStructuredRecordRepository recipeStructuredRecordRepository;
    private final RecipeSourceRecordRepository recipeSourceRecordRepository;
    private final RecipeDetailContentRepository recipeDetailContentRepository;
    private final RecipeDetailAiMapper recipeDetailAiMapper;
    private final RecipeDetailValidator recipeDetailValidator;

    @Override
    @Transactional
    public RecipeAiDetailGenerateResponse generateDetail(Long structuredRecordId) {
        RecipeStructuredRecord structuredRecord = recipeStructuredRecordRepository.findById(structuredRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "结构化记录不存在"));
        RecipeSourceRecord sourceRecord = structuredRecord.getSourceRecord();
        List<RecipeStructuredResultResponse.IngredientItemResponse> structuredIngredients = readStructuredIngredients(structuredRecord);
        List<RecipeStructuredResultResponse.StepItemResponse> structuredSteps = readStructuredSteps(structuredRecord);

        RecipeDetailPageResponse detail = canCallAi()
                ? generateByAi(structuredRecord, sourceRecord, structuredIngredients, structuredSteps)
                : buildMockDetail(structuredRecord, structuredIngredients, structuredSteps);
        RecipeDetailValidationResult validation = recipeDetailValidator.validate(detail);

        RecipeDetailContent content = recipeDetailContentRepository.findByStructuredRecordId(structuredRecordId)
                .orElseGet(RecipeDetailContent::new);
        applyDetailContent(content, structuredRecord, sourceRecord, detail);
        content.setStatus(RecipePipelineStatuses.PENDING_REVIEW);
        content.setAiGeneratedAt(LocalDateTime.now());
        RecipeDetailContent saved = recipeDetailContentRepository.save(content);

        structuredRecord.setStatus(RecipePipelineStatuses.AI_GENERATED);
        recipeStructuredRecordRepository.save(structuredRecord);
        sourceRecord.setStatus(RecipePipelineStatuses.AI_GENERATED);
        recipeSourceRecordRepository.save(sourceRecord);

        return toGenerateResponse(saved, detail, validation);
    }

    @Override
    public RecipeAiDetailGenerateResponse getDetailGeneration(Long detailContentId) {
        RecipeDetailContent content = recipeDetailContentRepository.findById(detailContentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AI详情结果不存在"));
        RecipeDetailPageResponse detail = toDetailPageResponse(content);
        return toGenerateResponse(content, detail, recipeDetailValidator.validate(detail));
    }

    @Override
    public List<RecipeAiDetailGenerateResponse> listDetailsByStatus(String status) {
        List<RecipeDetailContent> records;
        if (status == null || status.isBlank()) {
            records = recipeDetailContentRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        } else {
            records = recipeDetailContentRepository.findByStatusOrderByUpdatedAtDesc(status);
        }
        return records.stream()
                .map(content -> {
                    RecipeDetailPageResponse detail = toDetailPageResponse(content);
                    return toGenerateResponse(content, detail, recipeDetailValidator.validate(detail));
                })
                .toList();
    }

    private boolean canCallAi() {
        return !"mock".equalsIgnoreCase(aiConfig.getProvider())
                && aiQwenConfig.getApiKey() != null
                && !aiQwenConfig.getApiKey().isBlank();
    }

    private RecipeDetailPageResponse generateByAi(
            RecipeStructuredRecord structuredRecord,
            RecipeSourceRecord sourceRecord,
            List<RecipeStructuredResultResponse.IngredientItemResponse> structuredIngredients,
            List<RecipeStructuredResultResponse.StepItemResponse> structuredSteps
    ) {
        try {
            String factJson = objectMapper.writeValueAsString(buildFactPayload(structuredRecord, sourceRecord));
            Map<String, Object> body = Map.of(
                    "model", aiQwenConfig.getModel(),
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of(
                                            "role", "system",
                                            "content", "你是鸡尾酒内容编辑。请基于提供的结构化事实，生成一个严格符合 JSON 对象格式的详情页数据。" +
                                                    "不要输出 Markdown，不要输出代码块，不要补充事实层不存在且无法合理推导的内容。"
                                    ),
                                    Map.of(
                                            "role", "user",
                                            "content", buildAiPrompt(factJson)
                                    )
                            )
                    ),
                    "parameters", Map.of(
                            "temperature", 0.5,
                            "result_format", "message"
                    )
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiQwenConfig.getBaseUrl() + "/services/aigc/text-generation/generation"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiQwenConfig.getApiKey())
                    .timeout(Duration.ofMillis(aiQwenConfig.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("AI detail generation error: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.AI_ERROR, "AI详情生成失败");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractDashScopeContent(root);
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.AI_ERROR, "AI返回内容为空");
            }

            RecipeAiDetailCopyResult aiCopy = objectMapper.readValue(cleanAiJson(content), RecipeAiDetailCopyResult.class);
            return recipeDetailAiMapper.merge(structuredRecord, aiCopy, structuredIngredients, structuredSteps);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Generate recipe detail by AI failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI详情生成失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildFactPayload(RecipeStructuredRecord structuredRecord, RecipeSourceRecord sourceRecord) {
        return Map.ofEntries(
                Map.entry("recipeKey", structuredRecord.getRecipeKey()),
                Map.entry("englishName", structuredRecord.getEnglishName()),
                Map.entry("chineseNameDraft", safeString(structuredRecord.getChineseNameDraft())),
                Map.entry("category", safeString(structuredRecord.getCategory())),
                Map.entry("heroImage", safeString(structuredRecord.getHeroImage())),
                Map.entry("garnish", safeString(structuredRecord.getGarnish())),
                Map.entry("glassDraft", safeString(structuredRecord.getGlassDraft())),
                Map.entry("methodText", safeString(structuredRecord.getMethodText())),
                Map.entry("estimatedAbv", safeString(structuredRecord.getEstimatedAbv())),
                Map.entry("estimatedVolume", safeString(structuredRecord.getEstimatedVolume())),
                Map.entry("ingredients", readStructuredIngredients(structuredRecord)),
                Map.entry("steps", readStructuredSteps(structuredRecord)),
                Map.entry("sourceSite", safeString(sourceRecord.getSourceSite())),
                Map.entry("sourceUrl", safeString(sourceRecord.getSourceUrl()))
        );
    }

    private String buildAiPrompt(String factJson) {
        return "请基于下列结构化事实，只生成需要 AI 润色的展示字段 JSON 对象。" +
                "只允许输出这些字段：name、highlight、subtitle、description、story、bestFor、flavorTags、flavorMetrics、pairings、serviceNotes。" +
                "不要输出 id、englishName、category、heroImage、abv、volume、glass、garnish、ingredients、steps、difficulty、duration、serveTemperature。" +
                "其中 flavorTags 为字符串数组，flavorMetrics 为对象数组，pairings 与 serviceNotes 为字符串数组。" +
                "name 必须是中文名；subtitle、description、story、bestFor 需适合前端详情页展示，尽量简洁自然。" +
                "\n结构化事实如下：\n" + factJson;
    }

    private String extractDashScopeContent(JsonNode root) {
        String fromMessage = root.path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(null);
        if (fromMessage != null && !fromMessage.isBlank()) {
            return fromMessage;
        }
        return root.path("output").path("text").asText(null);
    }

    private String cleanAiJson(String content) {
        String cleaned = content.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private RecipeDetailPageResponse buildMockDetail(
            RecipeStructuredRecord structuredRecord,
            List<RecipeStructuredResultResponse.IngredientItemResponse> structuredIngredients,
            List<RecipeStructuredResultResponse.StepItemResponse> structuredSteps
    ) {
        RecipeAiDetailCopyResult fallbackCopy = recipeDetailAiMapper.buildMockCopy(structuredRecord);
        return recipeDetailAiMapper.merge(structuredRecord, fallbackCopy, structuredIngredients, structuredSteps);
    }

    private void applyDetailContent(
            RecipeDetailContent content,
            RecipeStructuredRecord structuredRecord,
            RecipeSourceRecord sourceRecord,
            RecipeDetailPageResponse detail
    ) {
        content.setStructuredRecord(structuredRecord);
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
        content.setSourceSite(sourceRecord.getSourceSite());
        content.setSourceUrl(sourceRecord.getSourceUrl());
    }

    private RecipeAiDetailGenerateResponse toGenerateResponse(
            RecipeDetailContent content,
            RecipeDetailPageResponse detail,
            RecipeDetailValidationResult validation
    ) {
        return RecipeAiDetailGenerateResponse.builder()
                .detailContentId(content.getId())
                .structuredRecordId(content.getStructuredRecord().getId())
                .recipeKey(content.getRecipeKey())
                .status(content.getStatus())
                .aiGeneratedAt(content.getAiGeneratedAt())
                .sourceSite(content.getSourceSite())
                .sourceUrl(content.getSourceUrl())
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

    private List<RecipeStructuredResultResponse.IngredientItemResponse> readStructuredIngredients(RecipeStructuredRecord record) {
        return readList(record.getIngredientsJson(), new TypeReference<List<RecipeStructuredResultResponse.IngredientItemResponse>>() {});
    }

    private List<RecipeStructuredResultResponse.StepItemResponse> readStructuredSteps(RecipeStructuredRecord record) {
        return readList(record.getStepsJson(), new TypeReference<List<RecipeStructuredResultResponse.StepItemResponse>>() {});
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "JSON解析失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "JSON序列化失败");
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
