package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;
import com.shakepro.service.AdminPageAiGenerateService;
import com.shakepro.service.support.QwenJsonCompletionClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPageAiGenerateServiceImpl implements AdminPageAiGenerateService {

    private static final String GENERATE_MODE = "QWEN_CN_TRANSLATE_AND_FILL";

    private final QwenJsonCompletionClient qwenJsonCompletionClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Override
    public AdminPageExtractFieldsResponse generateChineseFields(AdminPageExtractFieldsResponse extracted) {
        AdminPageExtractFieldsResponse aiDraft = callAiDraft(extracted);
        AdminPageExtractFieldsResponse merged = mergeAiResult(extracted, aiDraft);
        merged.setMissingFields(calculateMissingFields(merged));
        return merged;
    }

    private AdminPageExtractFieldsResponse callAiDraft(AdminPageExtractFieldsResponse extracted) {
        try {
            String content = qwenJsonCompletionClient.completeJson(buildSystemPrompt(), buildUserPrompt(extracted));
            logAiDraftIfDev("AI 原始返回", content);
            JsonNode root = objectMapper.readTree(content);
            ObjectNode normalizedRoot = normalizeAiDraftJson(root);
            logAiDraftIfDev("AI 归一化结果", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalizedRoot));
            return objectMapper.treeToValue(normalizedRoot, AdminPageExtractFieldsResponse.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 页面字段生成失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI中文字段生成失败: " + e.getMessage());
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一个专业的鸡尾酒内容编辑与翻译助手。
                你的任务是根据系统提供的已提取鸡尾酒字段，输出一个严格 JSON 对象。
                
                要求：
                
                1. 所有可翻译的人类可读字段都翻译成简体中文。
                2. 对解析阶段缺失的内容型字段，结合已有字段补充简体中文结果。
                3. 不得编造图片链接，不得修改 heroImage。
                4. flavorMetrics 保留数值，键名翻译成中文。
                5. ingredients 必须返回数组对象，元素结构为 { "name": "...", "amount": "...", "note": "..." }。
                   其中 amount 尽量沿用原始用量文本；note 是材料补充说明，用来解释这个材料为什么用、怎么选、要注意什么。
                6. steps 必须返回数组对象，元素结构为 { "title": "...", "detail": "..." }。
                7. englishName 尽量保留英文原名；如果原始 name 是英文且 englishName 缺失，可把原始英文名填入 englishName。
                8. 如果确实无法可靠补充某字段，可返回 null、[] 或 {}，不要输出解释文字。
                9. serviceNotes 必须返回字符串数组，每一项是一条服务或调制备注；如果没有可返回 []。
                10. 只返回 JSON 对象，不要输出 Markdown 代码块，不要输出额外说明。
                11. 以下字段必须返回字符串或 null，绝不能返回数组或对象：
                   name, englishName, category, heroImage, difficulty, abv, glass, garnish, highlight, subtitle, description, story。
                12. 为适配前端 UI，所有字段必须遵守以下字数与数量限制，超出时请主动压缩、改写或提炼：
                
                - name：2-8字
                - englishName：1-24字符
                - category：2-12字
                - heroImage：原样保留，不改写
                - difficulty：2-6字
                - abv：1-8字符
                - glass：2-12字
                - garnish：1-12字
                - highlight：10-26字
                - subtitle：16-32字
                - description：40-90字
                - story：50-120字
                - flavorTags：2-4项
                - flavorTags 每项：2-4字
                - flavorMetrics：3-5项
                - flavorMetrics 的键名：2-4字
                - flavorMetrics 的值：0-5 的数字
                - pairings：0-3项
                - pairings 每项：2-10字
                - serviceNotes：0-3项
                - serviceNotes 每项：10-32字
                - ingredients：3-8项
                - ingredients[].name：2-12字
                - ingredients[].amount：1-12字符
                - ingredients[].note：8-28字
                - steps：3-6项
                - steps[].title：2-8字
                - steps[].detail：16-44字
                
                13. 文案风格要求：
                
                - 面向普通用户，不要写“字段”“接口”“模型”“解析阶段”“开发者”“数据库”等面向开发的词。
                - 用语自然、克制、专业，像酒单或品牌内容，不要写得像百科或论文。
                - 优先简洁、易读、适合移动端卡片展示。
                
                JSON 字段必须包含：
                name, englishName, category, heroImage, difficulty, abv, glass, garnish, highlight, subtitle, description, story, flavorTags,
                flavorMetrics, pairings, serviceNotes, ingredients, steps
                """;
    }

    private String buildUserPrompt(AdminPageExtractFieldsResponse extracted) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("url", extracted.getUrl());
        payload.put("title", extracted.getTitle());
        payload.put("extractMode", extracted.getExtractMode());
        payload.put("name", extracted.getName());
        payload.put("englishName", extracted.getEnglishName());
        payload.put("category", extracted.getCategory());
        payload.put("heroImage", extracted.getHeroImage());
        payload.put("difficulty", extracted.getDifficulty());
        payload.put("abv", extracted.getAbv());
        payload.put("glass", extracted.getGlass());
        payload.put("garnish", extracted.getGarnish());
        payload.put("highlight", extracted.getHighlight());
        payload.put("subtitle", extracted.getSubtitle());
        payload.put("description", extracted.getDescription());
        payload.put("story", extracted.getStory());
        payload.put("flavorTags", extracted.getFlavorTags());
        payload.put("flavorMetrics", extracted.getFlavorMetrics());
        payload.put("pairings", extracted.getPairings());
        payload.put("serviceNotes", extracted.getServiceNotes());
        payload.put("ingredients", extracted.getIngredients());
        payload.put("steps", extracted.getSteps());
        payload.put("missingFields", extracted.getMissingFields());

        return "以下是解析器已提取的鸡尾酒字段，请输出最终中文结果 JSON：\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private ObjectNode normalizeAiDraftJson(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new BusinessException(ErrorCode.AI_ERROR, "AI返回的不是合法 JSON 对象");
        }

        ObjectNode normalized = ((ObjectNode) root).deepCopy();

        List<String> textFields = List.of(
                "name", "englishName", "category", "heroImage", "difficulty", "abv", "glass", "garnish",
                "highlight", "subtitle", "description", "story"
        );
        for (String fieldName : textFields) {
            writeNullableText(normalized, fieldName, readTextValue(root.get(fieldName)));
        }

        writeStringList(normalized, "flavorTags", readStringList(root.get("flavorTags")));
        writeStringList(normalized, "pairings", readStringList(root.get("pairings")));
        writeStringList(normalized, "serviceNotes", readStringList(root.get("serviceNotes")));
        writeFlavorMetrics(normalized, "flavorMetrics", root.get("flavorMetrics"));
        writeIngredients(normalized, "ingredients", root.get("ingredients"));
        writeSteps(normalized, "steps", root.get("steps"));

        return normalized;
    }

    private void logAiDraftIfDev(String label, String content) {
        if (!isDevProfile() || content == null) {
            return;
        }
        log.info("{}:\n{}", label, content);
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("dev"::equalsIgnoreCase);
    }

    private AdminPageExtractFieldsResponse mergeAiResult(
            AdminPageExtractFieldsResponse extracted,
            AdminPageExtractFieldsResponse aiDraft) {
        AdminPageExtractFieldsResponse merged = AdminPageExtractFieldsResponse.builder()
                .url(extracted.getUrl())
                .title(extracted.getTitle())
                .extractMode(extracted.getExtractMode())
                .generateMode(GENERATE_MODE)
                .name(mergeText(aiDraft.getName(), extracted.getName()))
                .englishName(mergeText(aiDraft.getEnglishName(), extracted.getEnglishName()))
                .category(mergeText(aiDraft.getCategory(), extracted.getCategory()))
                .heroImage(extracted.getHeroImage())
                .difficulty(mergeText(aiDraft.getDifficulty(), extracted.getDifficulty()))
                .abv(mergeText(aiDraft.getAbv(), extracted.getAbv()))
                .glass(mergeText(aiDraft.getGlass(), extracted.getGlass()))
                .garnish(mergeText(aiDraft.getGarnish(), extracted.getGarnish()))
                .highlight(mergeText(aiDraft.getHighlight(), extracted.getHighlight()))
                .subtitle(mergeText(aiDraft.getSubtitle(), extracted.getSubtitle()))
                .description(mergeText(aiDraft.getDescription(), extracted.getDescription()))
                .story(mergeText(aiDraft.getStory(), extracted.getStory()))
                .flavorTags(mergeStringList(aiDraft.getFlavorTags(), extracted.getFlavorTags()))
                .flavorMetrics(mergeFlavorMetrics(aiDraft.getFlavorMetrics(), extracted.getFlavorMetrics()))
                .pairings(mergeStringList(aiDraft.getPairings(), extracted.getPairings()))
                .serviceNotes(mergeStringList(aiDraft.getServiceNotes(), extracted.getServiceNotes()))
                .ingredients(mergeIngredients(aiDraft.getIngredients(), extracted.getIngredients()))
                .steps(mergeSteps(aiDraft.getSteps(), extracted.getSteps()))
                .fieldSources(buildAiFieldSources(extracted, aiDraft))
                .build();
        return merged;
    }

    private Map<String, AdminPageExtractFieldsResponse.FieldSource> buildAiFieldSources(
            AdminPageExtractFieldsResponse extracted,
            AdminPageExtractFieldsResponse aiDraft) {
        Map<String, AdminPageExtractFieldsResponse.FieldSource> sources = new LinkedHashMap<>();
        sources.put("name", buildAiFieldSource("name", extracted.getName(), aiDraft.getName(), originalFieldSource(extracted, "name")));
        sources.put("englishName", buildAiFieldSource("englishName", extracted.getEnglishName(), aiDraft.getEnglishName(), originalFieldSource(extracted, "englishName")));
        sources.put("category", buildAiFieldSource("category", extracted.getCategory(), aiDraft.getCategory(), originalFieldSource(extracted, "category")));
        sources.put("heroImage", retainedSource(originalFieldSource(extracted, "heroImage"), "图片链接沿用解析阶段结果"));
        sources.put("difficulty", buildAiFieldSource("difficulty", extracted.getDifficulty(), aiDraft.getDifficulty(), originalFieldSource(extracted, "difficulty")));
        sources.put("abv", buildAiFieldSource("abv", extracted.getAbv(), aiDraft.getAbv(), originalFieldSource(extracted, "abv")));
        sources.put("glass", buildAiFieldSource("glass", extracted.getGlass(), aiDraft.getGlass(), originalFieldSource(extracted, "glass")));
        sources.put("garnish", buildAiFieldSource("garnish", extracted.getGarnish(), aiDraft.getGarnish(), originalFieldSource(extracted, "garnish")));
        sources.put("highlight", buildAiFieldSource("highlight", extracted.getHighlight(), aiDraft.getHighlight(), originalFieldSource(extracted, "highlight")));
        sources.put("subtitle", buildAiFieldSource("subtitle", extracted.getSubtitle(), aiDraft.getSubtitle(), originalFieldSource(extracted, "subtitle")));
        sources.put("description", buildAiFieldSource("description", extracted.getDescription(), aiDraft.getDescription(), originalFieldSource(extracted, "description")));
        sources.put("story", buildAiFieldSource("story", extracted.getStory(), aiDraft.getStory(), originalFieldSource(extracted, "story")));
        sources.put("flavorTags", buildAiCollectionFieldSource(extracted.getFlavorTags(), aiDraft.getFlavorTags(), originalFieldSource(extracted, "flavorTags"), "flavorTags"));
        sources.put("flavorMetrics", buildAiMapFieldSource(extracted.getFlavorMetrics(), aiDraft.getFlavorMetrics(), originalFieldSource(extracted, "flavorMetrics"), "flavorMetrics"));
        sources.put("pairings", buildAiCollectionFieldSource(extracted.getPairings(), aiDraft.getPairings(), originalFieldSource(extracted, "pairings"), "pairings"));
        sources.put("serviceNotes", buildAiCollectionFieldSource(extracted.getServiceNotes(), aiDraft.getServiceNotes(), originalFieldSource(extracted, "serviceNotes"), "serviceNotes"));
        sources.put("ingredients", buildAiCollectionFieldSource(extracted.getIngredients(), aiDraft.getIngredients(), originalFieldSource(extracted, "ingredients"), "ingredients"));
        sources.put("steps", buildAiCollectionFieldSource(extracted.getSteps(), aiDraft.getSteps(), originalFieldSource(extracted, "steps"), "steps"));
        return sources;
    }

    private AdminPageExtractFieldsResponse.FieldSource originalFieldSource(
            AdminPageExtractFieldsResponse extracted,
            String fieldName) {
        if (extracted.getFieldSources() == null) {
            return null;
        }
        return extracted.getFieldSources().get(fieldName);
    }

    private AdminPageExtractFieldsResponse.FieldSource buildAiFieldSource(
            String fieldName,
            String extractedValue,
            String aiValue,
            AdminPageExtractFieldsResponse.FieldSource originalSource) {
        boolean hadExtracted = hasText(extractedValue);
        boolean hasAi = hasText(aiValue);

        if (!hadExtracted && !hasAi) {
            return missingSource("AI阶段仍未补全 " + fieldName);
        }
        if (!hadExtracted && hasAi) {
            return aiGeneratedSource(originalSource, "解析阶段缺失，由 AI 补全中文版");
        }
        if (hadExtracted && !hasAi) {
            return retainedSource(originalSource, "AI未返回该字段，沿用解析阶段结果");
        }
        if (aiValue.equals(extractedValue)) {
            return retainedSource(originalSource, "AI返回与解析阶段一致，沿用原值");
        }
        return aiTranslatedSource(originalSource, "基于解析阶段结果翻译或润色为中文");
    }

    private AdminPageExtractFieldsResponse.FieldSource buildAiCollectionFieldSource(
            Object extractedValue,
            Object aiValue,
            AdminPageExtractFieldsResponse.FieldSource originalSource,
            String fieldName) {
        boolean hadExtracted = hasCollectionValue(extractedValue);
        boolean hasAi = hasCollectionValue(aiValue);

        if (!hadExtracted && !hasAi) {
            return missingSource("AI阶段仍未补全 " + fieldName);
        }
        if (!hadExtracted && hasAi) {
            return aiGeneratedSource(originalSource, "解析阶段缺失，由 AI 生成中文版列表");
        }
        if (hadExtracted && !hasAi) {
            return retainedSource(originalSource, "AI未返回该列表，沿用解析阶段结果");
        }
        if (aiValue.equals(extractedValue)) {
            return retainedSource(originalSource, "AI返回与解析阶段一致，沿用原列表");
        }
        return aiTranslatedSource(originalSource, "基于解析阶段列表翻译或补充为中文");
    }

    private AdminPageExtractFieldsResponse.FieldSource buildAiMapFieldSource(
            Map<String, Integer> extractedValue,
            Map<String, Integer> aiValue,
            AdminPageExtractFieldsResponse.FieldSource originalSource,
            String fieldName) {
        boolean hadExtracted = extractedValue != null && !extractedValue.isEmpty();
        boolean hasAi = aiValue != null && !aiValue.isEmpty();

        if (!hadExtracted && !hasAi) {
            return missingSource("AI阶段仍未补全 " + fieldName);
        }
        if (!hadExtracted && hasAi) {
            return aiGeneratedSource(originalSource, "解析阶段缺失，由 AI 生成中文版风味指标");
        }
        if (hadExtracted && !hasAi) {
            return retainedSource(originalSource, "AI未返回风味指标，沿用解析阶段结果");
        }
        if (aiValue.equals(extractedValue)) {
            return retainedSource(originalSource, "AI返回与解析阶段一致，沿用原风味指标");
        }
        return aiTranslatedSource(originalSource, "基于解析阶段风味指标翻译键名为中文");
    }

    private String mergeText(String preferred, String fallback) {
        return hasText(preferred) ? preferred.trim() : normalizeNullableText(fallback);
    }

    private String readTextValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return normalizeNullableText(node.asText());
        }
        if (node.isArray()) {
            List<String> values = StreamSupport.stream(node.spliterator(), false)
                    .map(this::readTextValue)
                    .filter(this::hasText)
                    .toList();
            if (values.isEmpty()) {
                return null;
            }
            return String.join("、", values);
        }
        return null;
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(this::readTextValue)
                    .filter(this::hasText)
                    .toList();
        }
        String text = readTextValue(node);
        if (!hasText(text)) {
            return List.of();
        }
        return splitToList(text);
    }

    private Map<String, Integer> readFlavorMetrics(JsonNode node) {
        Map<String, Integer> metrics = new LinkedHashMap<>();
        if (node == null || node.isNull() || !node.isObject()) {
            return metrics;
        }

        node.fields().forEachRemaining(entry -> {
            String key = normalizeNullableText(entry.getKey());
            JsonNode valueNode = entry.getValue();
            if (key == null || valueNode == null || valueNode.isNull() || !valueNode.canConvertToInt()) {
                return;
            }
            metrics.put(key, valueNode.intValue());
        });
        return metrics;
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> readIngredients(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }

        List<AdminPageExtractFieldsResponse.IngredientItem> items = new ArrayList<>();
        for (JsonNode itemNode : node) {
            if (itemNode == null || itemNode.isNull()) {
                continue;
            }

            if (itemNode.isObject()) {
                String name = readTextValue(itemNode.get("name"));
                String amount = readTextValue(itemNode.get("amount"));
                String note = readTextValue(itemNode.get("note"));
                if (name != null || amount != null || note != null) {
                    items.add(AdminPageExtractFieldsResponse.IngredientItem.builder()
                            .name(name)
                            .amount(amount)
                            .note(note)
                            .build());
                }
                continue;
            }

            String text = readTextValue(itemNode);
            if (text != null) {
                items.add(AdminPageExtractFieldsResponse.IngredientItem.builder()
                        .name(text)
                        .amount(null)
                        .note(null)
                        .build());
            }
        }
        return items;
    }

    private List<AdminPageExtractFieldsResponse.StepItem> readSteps(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }

        List<AdminPageExtractFieldsResponse.StepItem> items = new ArrayList<>();
        for (JsonNode itemNode : node) {
            if (itemNode == null || itemNode.isNull()) {
                continue;
            }

            if (itemNode.isObject()) {
                String title = readTextValue(itemNode.get("title"));
                String detail = readTextValue(itemNode.get("detail"));
                if (title != null || detail != null) {
                    items.add(AdminPageExtractFieldsResponse.StepItem.builder()
                            .title(title)
                            .detail(detail)
                            .build());
                }
                continue;
            }

            String text = readTextValue(itemNode);
            if (text != null) {
                items.add(AdminPageExtractFieldsResponse.StepItem.builder()
                        .title(null)
                        .detail(text)
                        .build());
            }
        }
        return items;
    }

    private List<String> splitToList(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        return List.of(text.split("[\\n,，、/]+")).stream()
                .map(this::normalizeNullableText)
                .filter(value -> value != null)
                .toList();
    }

    private void writeNullableText(ObjectNode target, String fieldName, String value) {
        if (value == null) {
            target.putNull(fieldName);
            return;
        }
        target.put(fieldName, value);
    }

    private void writeStringList(ObjectNode target, String fieldName, List<String> values) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        values.forEach(arrayNode::add);
        target.set(fieldName, arrayNode);
    }

    private void writeFlavorMetrics(ObjectNode target, String fieldName, JsonNode sourceNode) {
        ObjectNode metricsNode = objectMapper.createObjectNode();
        readFlavorMetrics(sourceNode).forEach(metricsNode::put);
        target.set(fieldName, metricsNode);
    }

    private void writeIngredients(ObjectNode target, String fieldName, JsonNode sourceNode) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (AdminPageExtractFieldsResponse.IngredientItem item : readIngredients(sourceNode)) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            writeNullableText(itemNode, "name", item.getName());
            writeNullableText(itemNode, "amount", item.getAmount());
            writeNullableText(itemNode, "note", item.getNote());
            arrayNode.add(itemNode);
        }
        target.set(fieldName, arrayNode);
    }

    private void writeSteps(ObjectNode target, String fieldName, JsonNode sourceNode) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (AdminPageExtractFieldsResponse.StepItem item : readSteps(sourceNode)) {
            ObjectNode itemNode = objectMapper.createObjectNode();
            writeNullableText(itemNode, "title", item.getTitle());
            writeNullableText(itemNode, "detail", item.getDetail());
            arrayNode.add(itemNode);
        }
        target.set(fieldName, arrayNode);
    }

    private List<String> mergeStringList(List<String> preferred, List<String> fallback) {
        if (preferred != null && !preferred.isEmpty()) {
            return preferred.stream()
                    .map(this::normalizeNullableText)
                    .filter(value -> value != null)
                    .toList();
        }
        if (fallback == null) {
            return List.of();
        }
        return fallback.stream()
                .map(this::normalizeNullableText)
                .filter(value -> value != null)
                .toList();
    }

    private Map<String, Integer> mergeFlavorMetrics(Map<String, Integer> preferred, Map<String, Integer> fallback) {
        if (preferred != null && !preferred.isEmpty()) {
            return new LinkedHashMap<>(preferred);
        }
        if (fallback == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(fallback);
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> mergeIngredients(
            List<AdminPageExtractFieldsResponse.IngredientItem> preferred,
            List<AdminPageExtractFieldsResponse.IngredientItem> fallback) {
        if (preferred != null && !preferred.isEmpty()) {
            return preferred.stream()
                    .map(item -> AdminPageExtractFieldsResponse.IngredientItem.builder()
                            .name(normalizeNullableText(item.getName()))
                            .amount(normalizeNullableText(item.getAmount()))
                            .note(normalizeNullableText(item.getNote()))
                            .build())
                    .filter(item -> item.getName() != null || item.getAmount() != null || item.getNote() != null)
                    .toList();
        }
        if (fallback == null) {
            return List.of();
        }
        return new ArrayList<>(fallback);
    }

    private List<AdminPageExtractFieldsResponse.StepItem> mergeSteps(
            List<AdminPageExtractFieldsResponse.StepItem> preferred,
            List<AdminPageExtractFieldsResponse.StepItem> fallback) {
        if (preferred != null && !preferred.isEmpty()) {
            return preferred.stream()
                    .map(item -> AdminPageExtractFieldsResponse.StepItem.builder()
                            .title(normalizeNullableText(item.getTitle()))
                            .detail(normalizeNullableText(item.getDetail()))
                            .build())
                    .filter(item -> item.getTitle() != null || item.getDetail() != null)
                    .toList();
        }
        if (fallback == null) {
            return List.of();
        }
        return new ArrayList<>(fallback);
    }

    private List<String> calculateMissingFields(AdminPageExtractFieldsResponse response) {
        List<String> missingFields = new ArrayList<>();
        if (!hasText(response.getName())) {
            missingFields.add("name");
        }
        if (!hasText(response.getEnglishName())) {
            missingFields.add("englishName");
        }
        if (!hasText(response.getCategory())) {
            missingFields.add("category");
        }
        if (!hasText(response.getHeroImage())) {
            missingFields.add("heroImage");
        }
        if (!hasText(response.getDifficulty())) {
            missingFields.add("difficulty");
        }
        if (!hasText(response.getAbv())) {
            missingFields.add("abv");
        }
        if (!hasText(response.getGlass())) {
            missingFields.add("glass");
        }
        if (!hasText(response.getGarnish())) {
            missingFields.add("garnish");
        }
        if (!hasCollectionValue(response.getIngredients())) {
            missingFields.add("ingredients");
        }
        if (!hasCollectionValue(response.getSteps())) {
            missingFields.add("steps");
        }
        if (!hasText(response.getHighlight())) {
            missingFields.add("highlight");
        }
        if (!hasText(response.getSubtitle())) {
            missingFields.add("subtitle");
        }
        if (!hasText(response.getDescription())) {
            missingFields.add("description");
        }
        if (!hasText(response.getStory())) {
            missingFields.add("story");
        }
        if (!hasCollectionValue(response.getFlavorTags())) {
            missingFields.add("flavorTags");
        }
        if (response.getFlavorMetrics() == null || response.getFlavorMetrics().isEmpty()) {
            missingFields.add("flavorMetrics");
        }
        if (!hasCollectionValue(response.getPairings())) {
            missingFields.add("pairings");
        }
        if (!hasCollectionValue(response.getServiceNotes())) {
            missingFields.add("serviceNotes");
        }
        if (response.getIngredients() != null
                && !response.getIngredients().isEmpty()
                && response.getIngredients().stream().anyMatch(item -> !hasText(item.getNote()))) {
            missingFields.add("ingredients.note");
        }
        return missingFields;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasCollectionValue(Object value) {
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return false;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AdminPageExtractFieldsResponse.FieldSource aiTranslatedSource(
            AdminPageExtractFieldsResponse.FieldSource originalSource,
            String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("ai_translated")
                .source("ai.qwen")
                .note(appendOriginalSource(note, originalSource))
                .build();
    }

    private AdminPageExtractFieldsResponse.FieldSource aiGeneratedSource(
            AdminPageExtractFieldsResponse.FieldSource originalSource,
            String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("ai_generated")
                .source("ai.qwen")
                .note(appendOriginalSource(note, originalSource))
                .build();
    }

    private AdminPageExtractFieldsResponse.FieldSource retainedSource(
            AdminPageExtractFieldsResponse.FieldSource originalSource,
            String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("retained")
                .source(originalSource == null ? "parser" : normalizeNullableText(originalSource.getSource()))
                .note(appendOriginalSource(note, originalSource))
                .build();
    }

    private AdminPageExtractFieldsResponse.FieldSource missingSource(String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("missing")
                .source("ai.qwen")
                .note(note)
                .build();
    }

    private String appendOriginalSource(String note, AdminPageExtractFieldsResponse.FieldSource originalSource) {
        String normalizedNote = normalizeNullableText(note);
        if (originalSource == null) {
            return normalizedNote;
        }

        String originalMode = normalizeNullableText(originalSource.getMode());
        String original = normalizeNullableText(originalSource.getSource());
        StringBuilder builder = new StringBuilder(normalizedNote == null ? "" : normalizedNote);
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append("原始来源=");
        builder.append(originalMode == null ? "unknown" : originalMode);
        if (original != null) {
            builder.append("/").append(original);
        }
        return builder.toString();
    }
}
