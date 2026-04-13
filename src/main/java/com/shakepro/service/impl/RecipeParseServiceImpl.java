package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.RecipePipelineStatuses;
import com.shakepro.config.AiConfig;
import com.shakepro.config.AiQwenConfig;
import com.shakepro.dto.response.recipe.RecipeStructuredResultResponse;
import com.shakepro.entity.RecipeSourceRecord;
import com.shakepro.entity.RecipeStructuredRecord;
import com.shakepro.repository.RecipeSourceRecordRepository;
import com.shakepro.repository.RecipeStructuredRecordRepository;
import com.shakepro.service.RecipeParseService;
import lombok.Data;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeParseServiceImpl implements RecipeParseService {

    private static final int RAW_TEXT_LIMIT = 12000;
    private static final int RAW_HTML_LIMIT = 8000;

    private final RecipeSourceRecordRepository recipeSourceRecordRepository;
    private final RecipeStructuredRecordRepository recipeStructuredRecordRepository;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;
    private final AiQwenConfig aiQwenConfig;
    private final HttpClient aiHttpClient;

    @Override
    @Transactional
    public RecipeStructuredResultResponse parseSourceRecord(Long sourceRecordId) {
        RecipeSourceRecord sourceRecord = recipeSourceRecordRepository.findById(sourceRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "原始采集记录不存在"));

        if (RecipePipelineStatuses.REJECTED.equals(sourceRecord.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该原始记录已驳回，不能继续进入 AI 解析");
        }

        AiStructuredExtraction extraction = canCallAi()
                ? extractByAi(sourceRecord)
                : buildMockExtraction(sourceRecord);

        List<RecipeStructuredResultResponse.IngredientItemResponse> ingredients = sanitizeIngredients(extraction.getIngredients());
        List<RecipeStructuredResultResponse.StepItemResponse> steps = sanitizeSteps(extraction.getSteps());
        String englishName = firstNonBlank(extraction.getEnglishName(), fallbackEnglishName(sourceRecord));
        String recipeKey = normalizeKey(firstNonBlank(extraction.getRecipeKey(), englishName));

        RecipeStructuredRecord record = recipeStructuredRecordRepository.findBySourceRecordId(sourceRecordId)
                .orElseGet(RecipeStructuredRecord::new);
        record.setSourceRecord(sourceRecord);
        record.setRecipeKey(recipeKey);
        record.setEnglishName(englishName);
        record.setChineseNameDraft(trimToNull(extraction.getChineseNameDraft()));
        record.setCategory(trimToNull(extraction.getCategory()));
        record.setHeroImage(trimToNull(extraction.getHeroImage()));
        record.setGarnish(trimToNull(extraction.getGarnish()));
        record.setGlassDraft(trimToNull(extraction.getGlassDraft()));
        record.setMethodText(trimToNull(extraction.getMethodText()));
        record.setIngredientsJson(writeJson(ingredients));
        record.setStepsJson(writeJson(steps));
        record.setEstimatedAbv(trimToNull(extraction.getEstimatedAbv()));
        record.setEstimatedVolume(trimToNull(extraction.getEstimatedVolume()));
        record.setParseNotes(firstNonBlank(extraction.getParseNotes(), canCallAi()
                ? "已基于原始抓取文本完成 AI 结构化抽取，请继续检查事实层结果。"
                : "当前未接入结构化抽取 AI，已生成占位骨架，请人工补充事实字段。"));
        record.setStatus(RecipePipelineStatuses.PARSED);
        RecipeStructuredRecord saved = recipeStructuredRecordRepository.save(record);

        sourceRecord.setStatus(RecipePipelineStatuses.PARSED);
        recipeSourceRecordRepository.save(sourceRecord);

        return toResponse(saved, sourceRecord, ingredients, steps);
    }

    @Override
    public RecipeStructuredResultResponse getStructuredRecord(Long structuredRecordId) {
        RecipeStructuredRecord record = recipeStructuredRecordRepository.findById(structuredRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "结构化记录不存在"));
        RecipeSourceRecord sourceRecord = record.getSourceRecord();
        return toResponse(record, sourceRecord, readIngredients(record), readSteps(record));
    }

    @Override
    public List<RecipeStructuredResultResponse> listStructuredRecordsByStatus(String status) {
        List<RecipeStructuredRecord> records;
        if (status == null || status.isBlank()) {
            records = recipeStructuredRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "parsedAt"));
        } else {
            records = recipeStructuredRecordRepository.findByStatusOrderByParsedAtDesc(status);
        }
        return records.stream()
                .map(record -> toResponse(record, record.getSourceRecord(), readIngredients(record), readSteps(record)))
                .toList();
    }

    private boolean canCallAi() {
        return !"mock".equalsIgnoreCase(aiConfig.getProvider())
                && aiQwenConfig.getApiKey() != null
                && !aiQwenConfig.getApiKey().isBlank();
    }

    private AiStructuredExtraction extractByAi(RecipeSourceRecord sourceRecord) {
        try {
            String prompt = buildFactExtractionPrompt(sourceRecord);
            Map<String, Object> body = Map.of(
                    "model", aiQwenConfig.getModel(),
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of(
                                            "role", "system",
                                            "content", "你是鸡尾酒配方结构化抽取助手。你只能根据输入内容提取事实，禁止编造字段。"
                                    ),
                                    Map.of(
                                            "role", "user",
                                            "content", prompt
                                    )
                            )
                    ),
                    "parameters", Map.of(
                            "temperature", 0.2,
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
                log.error("Recipe fact extraction AI error: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.AI_ERROR, "AI 结构化抽取失败");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractDashScopeContent(root);
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.AI_ERROR, "AI 返回的结构化内容为空");
            }

            return objectMapper.readValue(cleanAiJson(content), AiStructuredExtraction.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Extract recipe facts by AI failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI 结构化抽取失败: " + e.getMessage());
        }
    }

    private String buildFactExtractionPrompt(RecipeSourceRecord sourceRecord) {
        String rawText = clip(sourceRecord.getRawText(), RAW_TEXT_LIMIT);
        String rawHtml = clip(sourceRecord.getRawHtml(), RAW_HTML_LIMIT);
        return "请从下面的网页抓取内容中抽取鸡尾酒配方事实，并严格输出 JSON 对象。"
                + "只允许输出以下字段：recipeKey、englishName、chineseNameDraft、category、heroImage、garnish、glassDraft、methodText、estimatedAbv、estimatedVolume、parseNotes、ingredients、steps。"
                + "其中 ingredients 为数组，每项只允许包含 name、amount、note、category。"
                + "steps 为数组，每项只允许包含 orderNo、title、detail、hint。"
                + "如果某字段无法确认，请返回空字符串或空数组，不要编造。"
                + "heroImage 必须是原页面里已有的图片 URL；estimatedAbv 与 estimatedVolume 允许根据配方做保守估算。"
                + "输出必须是 JSON，对象外不要有任何说明文字。"
                + "\n来源站点：" + safeString(sourceRecord.getSourceSite())
                + "\n来源地址：" + safeString(sourceRecord.getSourceUrl())
                + "\n网页纯文本：\n" + safeString(rawText)
                + "\n网页 HTML 片段：\n" + safeString(rawHtml);
    }

    private AiStructuredExtraction buildMockExtraction(RecipeSourceRecord sourceRecord) {
        AiStructuredExtraction extraction = new AiStructuredExtraction();
        extraction.setRecipeKey(normalizeKey(fallbackEnglishName(sourceRecord)));
        extraction.setEnglishName(fallbackEnglishName(sourceRecord));
        extraction.setParseNotes("当前未配置配方结构化抽取 AI，已生成基础骨架，请管理员补充事实字段后再继续。\n原文摘要："
                + clip(sourceRecord.getRawText(), 240));
        extraction.setIngredients(new ArrayList<>());
        extraction.setSteps(new ArrayList<>());
        return extraction;
    }

    private List<RecipeStructuredResultResponse.IngredientItemResponse> sanitizeIngredients(List<AiIngredientItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<RecipeStructuredResultResponse.IngredientItemResponse> result = new ArrayList<>();
        for (AiIngredientItem item : items) {
            if (item == null) {
                continue;
            }
            String name = trimToNull(item.getName());
            String amount = trimToNull(item.getAmount());
            String note = trimToNull(item.getNote());
            String category = trimToNull(item.getCategory());
            if (name == null && amount == null && note == null && category == null) {
                continue;
            }
            result.add(RecipeStructuredResultResponse.IngredientItemResponse.builder()
                    .name(name == null ? "待人工补充" : name)
                    .amount(amount == null ? "待确认" : amount)
                    .note(note)
                    .category(category)
                    .build());
        }
        return result;
    }

    private List<RecipeStructuredResultResponse.StepItemResponse> sanitizeSteps(List<AiStepItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }
        List<RecipeStructuredResultResponse.StepItemResponse> result = new ArrayList<>();
        int orderNo = 1;
        for (AiStepItem item : items) {
            if (item == null) {
                continue;
            }
            String title = trimToNull(item.getTitle());
            String detail = trimToNull(item.getDetail());
            String hint = trimToNull(item.getHint());
            if (title == null && detail == null && hint == null) {
                continue;
            }
            Integer aiOrder = item.getOrderNo();
            result.add(RecipeStructuredResultResponse.StepItemResponse.builder()
                    .orderNo(aiOrder == null || aiOrder <= 0 ? orderNo : aiOrder)
                    .title(title == null ? "步骤" + orderNo : title)
                    .detail(detail == null ? "待人工补充" : detail)
                    .hint(hint)
                    .build());
            orderNo++;
        }
        return result;
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

    private String fallbackEnglishName(RecipeSourceRecord sourceRecord) {
        String sourceUrl = trimToNull(sourceRecord.getSourceUrl());
        if (sourceUrl == null) {
            return "unknown-recipe";
        }
        String[] parts = sourceUrl.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = trimToNull(parts[i]);
            if (part != null) {
                return part.replace('-', ' ');
            }
        }
        return "unknown-recipe";
    }

    private RecipeStructuredResultResponse toResponse(
            RecipeStructuredRecord record,
            RecipeSourceRecord sourceRecord,
            List<RecipeStructuredResultResponse.IngredientItemResponse> ingredients,
            List<RecipeStructuredResultResponse.StepItemResponse> steps
    ) {
        return RecipeStructuredResultResponse.builder()
                .id(record.getId())
                .sourceRecordId(sourceRecord.getId())
                .recipeKey(record.getRecipeKey())
                .englishName(record.getEnglishName())
                .chineseNameDraft(record.getChineseNameDraft())
                .category(record.getCategory())
                .heroImage(record.getHeroImage())
                .garnish(record.getGarnish())
                .glassDraft(record.getGlassDraft())
                .methodText(record.getMethodText())
                .estimatedAbv(record.getEstimatedAbv())
                .estimatedVolume(record.getEstimatedVolume())
                .ingredients(ingredients)
                .steps(steps)
                .parseNotes(record.getParseNotes())
                .status(record.getStatus())
                .parsedAt(record.getParsedAt())
                .sourceSite(sourceRecord.getSourceSite())
                .sourceUrl(sourceRecord.getSourceUrl())
                .build();
    }

    private List<RecipeStructuredResultResponse.IngredientItemResponse> readIngredients(RecipeStructuredRecord record) {
        try {
            if (record.getIngredientsJson() == null || record.getIngredientsJson().isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(
                    record.getIngredientsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RecipeStructuredResultResponse.IngredientItemResponse.class)
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "解析结构化原料失败");
        }
    }

    private List<RecipeStructuredResultResponse.StepItemResponse> readSteps(RecipeStructuredRecord record) {
        try {
            if (record.getStepsJson() == null || record.getStepsJson().isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(
                    record.getStepsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RecipeStructuredResultResponse.StepItemResponse.class)
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "解析结构化步骤失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("Write json failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "结构化数据序列化失败");
        }
    }

    private String normalizeKey(String value) {
        return safeString(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String firstNonBlank(String first, String second) {
        String firstTrimmed = trimToNull(first);
        if (firstTrimmed != null) {
            return firstTrimmed;
        }
        return trimToNull(second);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String clip(String value, int limit) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= limit) {
            return trimmed;
        }
        return trimmed.substring(0, limit);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    @Data
    private static class AiStructuredExtraction {
        private String recipeKey;
        private String englishName;
        private String chineseNameDraft;
        private String category;
        private String heroImage;
        private String garnish;
        private String glassDraft;
        private String methodText;
        private String estimatedAbv;
        private String estimatedVolume;
        private String parseNotes;
        private List<AiIngredientItem> ingredients;
        private List<AiStepItem> steps;
    }

    @Data
    private static class AiIngredientItem {
        private String name;
        private String amount;
        private String note;
        private String category;
    }

    @Data
    private static class AiStepItem {
        private Integer orderNo;
        private String title;
        private String detail;
        private String hint;
    }
}
