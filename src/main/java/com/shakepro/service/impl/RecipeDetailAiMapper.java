package com.shakepro.service.impl;

import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import com.shakepro.dto.response.recipe.RecipeStructuredResultResponse;
import com.shakepro.entity.RecipeStructuredRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RecipeDetailAiMapper {

    public RecipeDetailPageResponse merge(
            RecipeStructuredRecord structuredRecord,
            RecipeAiDetailCopyResult aiCopy,
            List<RecipeStructuredResultResponse.IngredientItemResponse> structuredIngredients,
            List<RecipeStructuredResultResponse.StepItemResponse> structuredSteps
    ) {
        List<RecipeDetailPageResponse.IngredientItemResponse> ingredients = structuredIngredients.stream()
                .map(item -> RecipeDetailPageResponse.IngredientItemResponse.builder()
                        .name(item.getName())
                        .amount(item.getAmount())
                        .note(item.getNote())
                        .build())
                .toList();
        List<RecipeDetailPageResponse.StepItemResponse> steps = structuredSteps.stream()
                .map(item -> RecipeDetailPageResponse.StepItemResponse.builder()
                        .title(item.getTitle())
                        .detail(item.getDetail())
                        .hint(item.getHint())
                        .build())
                .toList();

        List<String> flavorTags = aiCopy == null ? null : aiCopy.getFlavorTags();
        if (flavorTags == null || flavorTags.isEmpty()) {
            flavorTags = buildDefaultFlavorTags(structuredRecord);
        }

        List<RecipeDetailPageResponse.FlavorMetricItemResponse> flavorMetrics = aiCopy == null ? null : aiCopy.getFlavorMetrics();
        if (flavorMetrics == null || flavorMetrics.isEmpty()) {
            flavorMetrics = buildDefaultFlavorMetrics();
        }

        return RecipeDetailPageResponse.builder()
                .id(structuredRecord.getRecipeKey())
                .name(defaultString(aiCopy == null ? null : aiCopy.getName(), defaultString(structuredRecord.getChineseNameDraft(), structuredRecord.getEnglishName())))
                .englishName(structuredRecord.getEnglishName())
                .category(defaultString(structuredRecord.getCategory(), "经典配方"))
                .heroImage(defaultString(structuredRecord.getHeroImage(), ""))
                .highlight(defaultString(aiCopy == null ? null : aiCopy.getHighlight(), "适合想快速了解这杯经典风味的人。"))
                .subtitle(defaultString(aiCopy == null ? null : aiCopy.getSubtitle(), "保留核心结构，适合作为前端详情页展示。"))
                .description(defaultString(aiCopy == null ? null : aiCopy.getDescription(), "根据外部配方整理出的标准化详情内容，可直接用于前端展示。"))
                .story(defaultString(aiCopy == null ? null : aiCopy.getStory(), "适合在聚会、餐前或想认真感受酒体结构时饮用。"))
                .bestFor(defaultString(aiCopy == null ? null : aiCopy.getBestFor(), "聚会、餐前"))
                .difficulty(guessDifficulty(ingredients, steps))
                .duration(guessDuration(steps))
                .abv(defaultString(structuredRecord.getEstimatedAbv(), "待估算"))
                .volume(defaultString(structuredRecord.getEstimatedVolume(), "待估算"))
                .glass(defaultString(structuredRecord.getGlassDraft(), "鸡尾酒杯"))
                .garnish(defaultString(structuredRecord.getGarnish(), "按喜好装饰"))
                .serveTemperature(guessServeTemperature(structuredRecord))
                .flavorTags(flavorTags)
                .flavorMetrics(flavorMetrics)
                .pairings(defaultList(aiCopy == null ? null : aiCopy.getPairings(), List.of("咸味小食")))
                .serviceNotes(defaultList(aiCopy == null ? null : aiCopy.getServiceNotes(), List.of("建议提前冷杯", "保持材料比例稳定")))
                .ingredients(ingredients)
                .steps(steps)
                .build();
    }

    public RecipeAiDetailCopyResult buildMockCopy(RecipeStructuredRecord structuredRecord) {
        return RecipeAiDetailCopyResult.builder()
                .name(defaultString(structuredRecord.getChineseNameDraft(), structuredRecord.getEnglishName()))
                .highlight("适合先快速浏览核心风味与配方结构。")
                .subtitle("将外部网页内容整理成适合前端展示的详情字段。")
                .description("以原始配方为基础，保留材料结构与制作步骤，便于在 App 中直接展示。")
                .story("这是一条由采集管线整理出的候选配方，适合继续审核后发布。")
                .bestFor("聚会、小酌")
                .flavorTags(buildDefaultFlavorTags(structuredRecord))
                .flavorMetrics(buildDefaultFlavorMetrics())
                .pairings(List.of("咸味坚果", "轻食小点"))
                .serviceNotes(List.of("建议提前冷杯", "出杯前再次确认稀释度"))
                .build();
    }

    private List<String> buildDefaultFlavorTags(RecipeStructuredRecord structuredRecord) {
        List<String> tags = new ArrayList<>();
        String category = safeString(structuredRecord.getCategory()).toLowerCase(Locale.ROOT);
        if (category.contains("unforgettable") || category.contains("classic")) {
            tags.add("经典");
        }
        if (structuredRecord.getGarnish() != null && structuredRecord.getGarnish().toLowerCase(Locale.ROOT).contains("mint")) {
            tags.add("草本");
        }
        tags.add("平衡");
        if (tags.size() < 3) {
            tags.add("清爽");
        }
        return tags.stream().distinct().limit(4).toList();
    }

    private List<RecipeDetailPageResponse.FlavorMetricItemResponse> buildDefaultFlavorMetrics() {
        return List.of(
                RecipeDetailPageResponse.FlavorMetricItemResponse.builder().label("酒感").value(4).build(),
                RecipeDetailPageResponse.FlavorMetricItemResponse.builder().label("清爽").value(3).build(),
                RecipeDetailPageResponse.FlavorMetricItemResponse.builder().label("酸度").value(2).build(),
                RecipeDetailPageResponse.FlavorMetricItemResponse.builder().label("甜感").value(2).build()
        );
    }

    private String guessDifficulty(
            List<RecipeDetailPageResponse.IngredientItemResponse> ingredients,
            List<RecipeDetailPageResponse.StepItemResponse> steps
    ) {
        int score = ingredients.size() + steps.size();
        if (score >= 10) {
            return "进阶";
        }
        if (score >= 6) {
            return "中等";
        }
        return "入门";
    }

    private String guessDuration(List<RecipeDetailPageResponse.StepItemResponse> steps) {
        int minutes = Math.max(2, Math.min(6, steps.size() + 1));
        return minutes + "分钟";
    }

    private String guessServeTemperature(RecipeStructuredRecord structuredRecord) {
        String methodText = safeString(structuredRecord.getMethodText()).toLowerCase(Locale.ROOT);
        if (methodText.contains("hot") || methodText.contains("warm")) {
            return "温热饮用";
        }
        if (methodText.contains("shake") || methodText.contains("stir") || methodText.contains("chilled")) {
            return "冰镇后饮用";
        }
        return "常温或加冰";
    }

    private <T> List<T> defaultList(List<T> current, List<T> fallback) {
        if (current == null || current.isEmpty()) {
            return fallback;
        }
        return current;
    }

    private String defaultString(String current, String fallback) {
        if (current == null || current.isBlank()) {
            return fallback;
        }
        return current;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
