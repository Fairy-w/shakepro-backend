package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.request.admin.AdminPageFieldExtractRequest;
import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;
import com.shakepro.service.AdminPageExtractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPageExtractServiceImpl implements AdminPageExtractService {

    private static final Pattern JSON_LD_PATTERN = Pattern.compile(
            "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern NEXT_DATA_PATTERN = Pattern.compile(
            "<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern WRAPPED_QUOTES_PATTERN = Pattern.compile("^\"(.*)\"$");
    private static final Pattern AMOUNT_PREFIX_PATTERN = Pattern.compile(
            "^(?<amount>(?:\\d+[\\d/.,\\s-]*|\\d+\\/\\d+|one|two|three|a|an)\\s*(?:oz|ounce|ounces|ml|cl|cup|cups|dash|dashes|barspoon|barspoons|teaspoon|teaspoons|tablespoon|tablespoons|part|parts|slice|slices|piece|pieces|drop|drops)?(?:\\s*\\([^)]*\\))?)\\s+(?<name>.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_BREAK_PATTERN = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_BLOCK_CLOSE_PATTERN = Pattern.compile("(?i)</(p|div|section|article|li|h[1-6])>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\t\\x0B\\f\\r ]+");
    private static final Pattern MULTI_LINE_BREAK_PATTERN = Pattern.compile("\\n{2,}");
    private static final Pattern BARTENDER_TIP_PATTERN = Pattern.compile("(?is)bartender\\s*tip\\s*:?\\s*(.+)$");
    private static final Pattern ABV_LABELED_PATTERN = Pattern.compile(
            "(?is)(?:abv|alcohol\\s*by\\s*volume|alcohol\\s*content|alcohol\\s*level)[^\\d%]{0,40}(\\d{1,2}(?:\\.\\d{1,2})?\\s*%)"
    );
    private static final Pattern OG_IMAGE_META_PATTERN = Pattern.compile(
            "(?is)<meta[^>]*\\bproperty=[\"']og:image[\"'][^>]*\\bcontent=[\"']([^\"']+)[\"'][^>]*>"
                    + "|<meta[^>]*\\bcontent=[\"']([^\"']+)[\"'][^>]*\\bproperty=[\"']og:image[\"'][^>]*>"
    );

    private final ObjectMapper objectMapper;

    @Override
    public AdminPageExtractFieldsResponse extractFields(AdminPageFieldExtractRequest request) {
        String html = request.getHtml() == null ? "" : request.getHtml().trim();
        if (html.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "HTML原文不能为空");
        }

        JsonNode recipeJsonLd = findRecipeJsonLd(html);
        JsonNode nextData = findNextData(html);
        String description = extractDescription(recipeJsonLd, nextData);
        String difficulty = extractDifficulty(nextData);
        String abv = extractAbv(nextData, html);
        List<AdminPageExtractFieldsResponse.IngredientItem> ingredients = parseIngredients(recipeJsonLd, nextData);
        List<AdminPageExtractFieldsResponse.StepItem> steps = parseSteps(recipeJsonLd, nextData);
        Map<String, Integer> flavorMetrics = extractFlavorMetrics(nextData);

        Map<String, AdminPageExtractFieldsResponse.FieldSource> fieldSources = new LinkedHashMap<>();
        fieldSources.put("name", resolveNameSource(recipeJsonLd, nextData));
        fieldSources.put("englishName", missingSource("当前站点解析器未实现英文名提取"));
        fieldSources.put("category", missingSource("当前站点解析器未实现分类提取"));
        fieldSources.put("heroImage", resolveHeroImageSource(recipeJsonLd, html));
        fieldSources.put("difficulty", resolveDifficultySource(nextData, difficulty));
        fieldSources.put("abv", resolveAbvSource(nextData, html, abv));
        fieldSources.put("glass", resolveGlassSource(nextData));
        fieldSources.put("garnish", resolveGarnishSource(nextData));
        fieldSources.put("highlight", missingSource("当前站点解析器未实现 highlight 提取"));
        fieldSources.put("subtitle", missingSource("当前站点解析器未实现 subtitle 提取"));
        fieldSources.put("description", resolveDescriptionSource(recipeJsonLd, nextData));
        fieldSources.put("story", missingSource("当前站点解析器未实现 story 提取"));
        fieldSources.put("flavorTags", missingSource("当前站点解析器未实现 flavorTags 提取"));
        fieldSources.put("flavorMetrics", resolveFlavorMetricsSource(nextData, flavorMetrics));
        fieldSources.put("pairings", missingSource("当前站点解析器未实现 pairings 提取"));
        List<String> serviceNotes = extractServiceNotes(description);
        fieldSources.put("serviceNotes", resolveServiceNotesSource(serviceNotes));
        fieldSources.put("ingredients", resolveIngredientsSource(recipeJsonLd, nextData, ingredients));
        fieldSources.put("steps", resolveStepsSource(recipeJsonLd, nextData, steps));

        AdminPageExtractFieldsResponse response = AdminPageExtractFieldsResponse.builder()
                .url(trimToNull(request.getUrl()))
                .title(trimToNull(request.getTitle()))
                .extractMode("COCKTAIL_CLUB_PARSER")
                .generateMode(null)
                .name(extractName(recipeJsonLd, nextData))
                .englishName(null)
                .category(null)
                .heroImage(extractHeroImage(recipeJsonLd, html))
                .difficulty(difficulty)
                .abv(abv)
                .glass(extractGlass(nextData))
                .garnish(extractGarnish(nextData))
                .highlight(null)
                .subtitle(null)
                .description(description)
                .story(null)
                .flavorTags(List.of())
                .flavorMetrics(flavorMetrics)
                .pairings(List.of())
                .serviceNotes(serviceNotes)
                .ingredients(ingredients)
                .steps(steps)
                .fieldSources(fieldSources)
                .build();

        response.setMissingFields(calculateMissingFields(response));
        return normalizeResponse(response);
    }

    private JsonNode findRecipeJsonLd(String html) {
        Matcher matcher = JSON_LD_PATTERN.matcher(html);
        while (matcher.find()) {
            String content = matcher.group(1);
            try {
                JsonNode node = objectMapper.readTree(content);
                JsonNode recipeNode = findRecipeNode(node);
                if (recipeNode != null) {
                    return recipeNode;
                }
            } catch (IOException e) {
                log.debug("跳过无法解析的 JSON-LD 脚本", e);
            }
        }
        return null;
    }

    private JsonNode findRecipeNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode recipeNode = findRecipeNode(item);
                if (recipeNode != null) {
                    return recipeNode;
                }
            }
            return null;
        }

        String type = textOrNull(node.get("@type"));
        if (type != null && type.toLowerCase(Locale.ROOT).contains("recipe")) {
            return node;
        }

        JsonNode graph = node.get("@graph");
        if (graph != null) {
            return findRecipeNode(graph);
        }
        return null;
    }

    private JsonNode findNextData(String html) {
        Matcher matcher = NEXT_DATA_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }

        try {
            return objectMapper.readTree(matcher.group(1));
        } catch (IOException e) {
            log.warn("解析 __NEXT_DATA__ 失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractName(JsonNode recipeJsonLd, JsonNode nextData) {
        String fromJsonLd = cleanQuotedText(recipeJsonLd == null ? null : recipeJsonLd.get("name"));
        if (fromJsonLd != null) {
            return fromJsonLd;
        }
        return textAt(nextData, "props", "pageProps", "cocktail", "name");
    }

    private String extractHeroImage(JsonNode recipeJsonLd, String html) {
        String fromJsonLd = extractHeroImageFromRecipeJsonLd(recipeJsonLd);
        String fromMeta = extractOpenGraphImage(html);
        if (fromJsonLd != null && isLikelyVideoThumbnailUrl(fromJsonLd) && fromMeta != null) {
            return fromMeta;
        }
        if (fromJsonLd != null) {
            return fromJsonLd;
        }
        return fromMeta;
    }

    private String extractHeroImageFromRecipeJsonLd(JsonNode recipeJsonLd) {
        if (recipeJsonLd == null) {
            return null;
        }

        JsonNode imageNode = recipeJsonLd.get("image");
        if (imageNode == null || imageNode.isNull()) {
            return null;
        }

        if (imageNode.isTextual()) {
            return cleanQuotedText(imageNode);
        }

        return cleanQuotedText(imageNode.get("url"));
    }

    private String extractOpenGraphImage(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher matcher = OG_IMAGE_META_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return trimToNull(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
    }

    private boolean isLikelyVideoThumbnailUrl(String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        return lower.contains("wistia")
                || lower.contains("/deliveries/")
                || lower.contains("video");
    }

    private String extractGlass(JsonNode nextData) {
        return textAt(nextData, "props", "pageProps", "cocktail", "glass", "name");
    }

    private String extractDifficulty(JsonNode nextData) {
        return textAt(nextData, "props", "pageProps", "cocktail", "difficulty", "name");
    }

    private String extractAbv(JsonNode nextData, String html) {
        String fromNextData = textAtAny(nextData,
                new String[]{"props", "pageProps", "cocktail", "abv"},
                new String[]{"props", "pageProps", "cocktail", "alcoholLevel"},
                new String[]{"props", "pageProps", "cocktail", "alcohol_level"},
                new String[]{"props", "pageProps", "cocktail", "alcoholContent"}
        );
        if (fromNextData != null) {
            return normalizeAbv(fromNextData);
        }

        if (html == null) {
            return null;
        }
        Matcher matcher = ABV_LABELED_PATTERN.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return normalizeAbv(matcher.group(1));
    }

    private String extractGarnish(JsonNode nextData) {
        JsonNode stepsNode = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("recipes").path(0).path("timeline").path("steps");
        if (stepsNode == null || !stepsNode.isArray()) {
            return null;
        }

        for (JsonNode stepNode : stepsNode) {
            String title = textOrNull(stepNode.get("title"));
            String description = textOrNull(stepNode.get("description"));
            String combined = ((title == null ? "" : title) + " " + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
            if (!combined.contains("garnish")) {
                continue;
            }

            List<String> names = new ArrayList<>();
            JsonNode ingredientsNode = stepNode.get("ingredients");
            if (ingredientsNode != null && ingredientsNode.isArray()) {
                for (JsonNode ingredientNode : ingredientsNode) {
                    String ingredientName = textOrNull(ingredientNode.get("name"));
                    if (ingredientName != null) {
                        names.add(ingredientName);
                    }
                }
            }

            if (!names.isEmpty()) {
                return joinNames(names);
            }
        }

        return null;
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> parseIngredients(JsonNode recipeJsonLd, JsonNode nextData) {
        List<AdminPageExtractFieldsResponse.IngredientItem> ingredients = new ArrayList<>();

        JsonNode ingredientArray = recipeJsonLd == null ? null : recipeJsonLd.get("recipeIngredient");
        if (ingredientArray != null && ingredientArray.isArray()) {
            for (JsonNode ingredientNode : ingredientArray) {
                String ingredientLine = cleanQuotedText(ingredientNode);
                if (ingredientLine != null) {
                    ingredients.add(parseIngredientLine(ingredientLine));
                }
            }
        }

        if (!ingredients.isEmpty()) {
            return deduplicateIngredients(ingredients);
        }

        JsonNode stepsNode = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("recipes").path(0).path("timeline").path("steps");
        if (stepsNode != null && stepsNode.isArray()) {
            for (JsonNode stepNode : stepsNode) {
                JsonNode stepIngredients = stepNode.get("ingredients");
                if (stepIngredients == null || !stepIngredients.isArray()) {
                    continue;
                }

                for (JsonNode ingredientNode : stepIngredients) {
                    String name = textOrNull(ingredientNode.get("name"));
                    JsonNode measurementsNode = ingredientNode.path("measurements").path(0);
                    String amount = buildMeasurement(measurementsNode);
                    if (name != null) {
                        ingredients.add(AdminPageExtractFieldsResponse.IngredientItem.builder()
                                .name(name)
                                .amount(amount)
                                .note(null)
                                .build());
                    }
                }
            }
        }

        return deduplicateIngredients(ingredients);
    }

    private AdminPageExtractFieldsResponse.IngredientItem parseIngredientLine(String ingredientLine) {
        String normalizedLine = ingredientLine.trim();
        Matcher matcher = AMOUNT_PREFIX_PATTERN.matcher(normalizedLine);
        if (matcher.matches()) {
            return AdminPageExtractFieldsResponse.IngredientItem.builder()
                    .amount(trimToNull(matcher.group("amount")))
                    .name(trimToNull(matcher.group("name")))
                    .note(null)
                    .build();
        }

        return AdminPageExtractFieldsResponse.IngredientItem.builder()
                .name(normalizedLine)
                .amount(null)
                .note(null)
                .build();
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> deduplicateIngredients(
            List<AdminPageExtractFieldsResponse.IngredientItem> ingredients) {
        List<AdminPageExtractFieldsResponse.IngredientItem> deduplicated = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (AdminPageExtractFieldsResponse.IngredientItem ingredient : ingredients) {
            String name = trimToNull(ingredient.getName());
            String amount = trimToNull(ingredient.getAmount());
            if (name == null) {
                continue;
            }

            String key = name + "|" + (amount == null ? "" : amount);
            if (!seen.add(key)) {
                continue;
            }

            deduplicated.add(AdminPageExtractFieldsResponse.IngredientItem.builder()
                    .name(name)
                    .amount(amount)
                    .note(null)
                    .build());
        }

        return deduplicated;
    }

    private List<AdminPageExtractFieldsResponse.StepItem> parseSteps(JsonNode recipeJsonLd, JsonNode nextData) {
        List<AdminPageExtractFieldsResponse.StepItem> steps = new ArrayList<>();

        JsonNode instructionArray = recipeJsonLd == null ? null : recipeJsonLd.get("recipeInstructions");
        if (instructionArray != null && instructionArray.isArray()) {
            for (JsonNode instructionNode : instructionArray) {
                String title = cleanQuotedText(instructionNode.get("name"));
                String detail = cleanQuotedText(instructionNode.get("text"));
                if (detail == null && instructionNode.isTextual()) {
                    detail = cleanQuotedText(instructionNode);
                }

                if (title != null || detail != null) {
                    steps.add(AdminPageExtractFieldsResponse.StepItem.builder()
                            .title(title)
                            .detail(detail)
                            .build());
                }
            }
        }

        if (!steps.isEmpty()) {
            return steps;
        }

        JsonNode stepArray = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("recipes").path(0).path("timeline").path("steps");
        if (stepArray != null && stepArray.isArray()) {
            for (JsonNode stepNode : stepArray) {
                String title = textOrNull(stepNode.get("title"));
                String detail = textOrNull(stepNode.get("description"));
                if (title != null || detail != null) {
                    steps.add(AdminPageExtractFieldsResponse.StepItem.builder()
                            .title(title)
                            .detail(detail)
                            .build());
                }
            }
        }

        return steps;
    }

    private String extractDescription(JsonNode recipeJsonLd, JsonNode nextData) {
        String jsonLdDescription = cleanQuotedText(recipeJsonLd == null ? null : recipeJsonLd.get("description"));
        if (jsonLdDescription != null) {
            return normalizePlainText(jsonLdDescription);
        }

        String descriptionHtml = textAt(nextData, "props", "pageProps", "cocktail", "description");
        if (descriptionHtml == null) {
            return null;
        }
        return htmlToPlainText(descriptionHtml);
    }

    private Map<String, Integer> extractFlavorMetrics(JsonNode nextData) {
        Map<String, Integer> flavorMetrics = new LinkedHashMap<>();
        JsonNode flavorsNode = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("flavors");
        if (flavorsNode == null || !flavorsNode.isArray()) {
            return flavorMetrics;
        }

        for (JsonNode flavorNode : flavorsNode) {
            String flavorName = textOrNull(flavorNode.path("flavor").get("name"));
            JsonNode valueNode = flavorNode.get("value");
            if (flavorName == null || valueNode == null || !valueNode.canConvertToInt()) {
                continue;
            }
            flavorMetrics.put(flavorName, valueNode.intValue());
        }
        return flavorMetrics;
    }

    private List<String> extractServiceNotes(String description) {
        if (description == null) {
            return List.of();
        }

        Matcher matcher = BARTENDER_TIP_PATTERN.matcher(description);
        if (!matcher.find()) {
            return List.of();
        }
        String note = normalizePlainText(matcher.group(1));
        if (note == null) {
            return List.of();
        }
        return List.of(note);
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveNameSource(JsonNode recipeJsonLd, JsonNode nextData) {
        if (cleanQuotedText(recipeJsonLd == null ? null : recipeJsonLd.get("name")) != null) {
            return extractedSource("jsonld", "来自 Recipe JSON-LD 的 name");
        }
        if (textAt(nextData, "props", "pageProps", "cocktail", "name") != null) {
            return extractedSource("nextData", "来自 __NEXT_DATA__.props.pageProps.cocktail.name");
        }
        return missingSource("未找到 name");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveHeroImageSource(JsonNode recipeJsonLd, String html) {
        String fromJsonLd = extractHeroImageFromRecipeJsonLd(recipeJsonLd);
        String fromMeta = extractOpenGraphImage(html);
        if (fromJsonLd != null && isLikelyVideoThumbnailUrl(fromJsonLd) && fromMeta != null) {
            return extractedSource("meta", "检测到 JSON-LD 图片疑似视频缩略图，改用 og:image");
        }
        if (fromJsonLd != null) {
            return extractedSource("jsonld", "来自 Recipe JSON-LD 的 image.url");
        }
        if (fromMeta != null) {
            return extractedSource("meta", "来自页面元信息 og:image");
        }
        return missingSource("未找到 heroImage");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveDifficultySource(JsonNode nextData, String difficulty) {
        if (difficulty != null && textAt(nextData, "props", "pageProps", "cocktail", "difficulty", "name") != null) {
            return extractedSource("nextData", "来自 __NEXT_DATA__.props.pageProps.cocktail.difficulty.name");
        }
        return missingSource("未找到 difficulty");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveAbvSource(JsonNode nextData, String html, String abv) {
        if (abv == null) {
            return missingSource("未找到 abv");
        }

        if (textAtAny(nextData,
                new String[]{"props", "pageProps", "cocktail", "abv"},
                new String[]{"props", "pageProps", "cocktail", "alcoholLevel"},
                new String[]{"props", "pageProps", "cocktail", "alcohol_level"},
                new String[]{"props", "pageProps", "cocktail", "alcoholContent"}) != null) {
            return extractedSource("nextData", "来自 __NEXT_DATA__ 的 abv/alcoholLevel 字段");
        }

        if (html != null && ABV_LABELED_PATTERN.matcher(html).find()) {
            return derivedSource("parser", "根据页面中带 ABV/Alcohol 标签的百分比文本提取得到");
        }

        return missingSource("未找到 abv");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveGlassSource(JsonNode nextData) {
        if (extractGlass(nextData) != null) {
            return extractedSource("nextData", "来自 __NEXT_DATA__.props.pageProps.cocktail.glass.name");
        }
        return missingSource("未找到 glass");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveGarnishSource(JsonNode nextData) {
        if (extractGarnish(nextData) != null) {
            return derivedSource("nextData", "根据 __NEXT_DATA__ 的 garnish 步骤与配料拼接得到");
        }
        return missingSource("未找到 garnish");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveDescriptionSource(JsonNode recipeJsonLd, JsonNode nextData) {
        if (cleanQuotedText(recipeJsonLd == null ? null : recipeJsonLd.get("description")) != null) {
            return extractedSource("jsonld", "来自 Recipe JSON-LD 的 description");
        }
        if (textAt(nextData, "props", "pageProps", "cocktail", "description") != null) {
            return derivedSource("nextData", "来自 __NEXT_DATA__.props.pageProps.cocktail.description，经 HTML 转纯文本");
        }
        return missingSource("未找到 description");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveFlavorMetricsSource(
            JsonNode nextData,
            Map<String, Integer> flavorMetrics) {
        JsonNode flavorsNode = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("flavors");
        if (flavorsNode != null && flavorsNode.isArray() && flavorMetrics != null && !flavorMetrics.isEmpty()) {
            return extractedSource("nextData", "来自 __NEXT_DATA__.props.pageProps.cocktail.flavors");
        }
        return missingSource("未找到 flavorMetrics");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveServiceNotesSource(List<String> serviceNotes) {
        if (!serviceNotes.isEmpty()) {
            return derivedSource("description", "根据 description 中的 Bartender Tip 片段提取得到");
        }
        return missingSource("未找到 serviceNotes");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveIngredientsSource(
            JsonNode recipeJsonLd,
            JsonNode nextData,
            List<AdminPageExtractFieldsResponse.IngredientItem> ingredients) {
        JsonNode ingredientArray = recipeJsonLd == null ? null : recipeJsonLd.get("recipeIngredient");
        if (ingredientArray != null && ingredientArray.isArray() && ingredients != null && !ingredients.isEmpty()) {
            return extractedSource("jsonld", "来自 Recipe JSON-LD 的 recipeIngredient");
        }

        JsonNode stepsNode = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("recipes").path(0).path("timeline").path("steps");
        if (stepsNode != null && stepsNode.isArray() && ingredients != null && !ingredients.isEmpty()) {
            return derivedSource("nextData", "来自 __NEXT_DATA__ 的 timeline.steps[].ingredients");
        }

        return missingSource("未找到 ingredients");
    }

    private AdminPageExtractFieldsResponse.FieldSource resolveStepsSource(
            JsonNode recipeJsonLd,
            JsonNode nextData,
            List<AdminPageExtractFieldsResponse.StepItem> steps) {
        JsonNode instructionArray = recipeJsonLd == null ? null : recipeJsonLd.get("recipeInstructions");
        if (instructionArray != null && instructionArray.isArray() && steps != null && !steps.isEmpty()) {
            return extractedSource("jsonld", "来自 Recipe JSON-LD 的 recipeInstructions");
        }

        JsonNode stepArray = nextData == null
                ? null
                : nextData.path("props").path("pageProps").path("cocktail").path("recipes").path(0).path("timeline").path("steps");
        if (stepArray != null && stepArray.isArray() && steps != null && !steps.isEmpty()) {
            return extractedSource("nextData", "来自 __NEXT_DATA__ 的 timeline.steps");
        }

        return missingSource("未找到 steps");
    }

    private String buildMeasurement(JsonNode measurementNode) {
        if (measurementNode == null || measurementNode.isMissingNode() || measurementNode.isNull()) {
            return null;
        }

        String amount = textOrNull(measurementNode.get("metric_amount"));
        String unit = textOrNull(measurementNode.path("metric_unit").get("name"));
        String suffix = textOrNull(measurementNode.get("suffix"));

        StringBuilder builder = new StringBuilder();
        if (amount != null) {
            builder.append(amount);
        }
        if (unit != null) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(unit);
        }
        if (suffix != null) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(suffix);
        }

        return trimToNull(builder.toString());
    }

    private String textAt(JsonNode node, String... paths) {
        JsonNode current = node;
        for (String path : paths) {
            if (current == null) {
                return null;
            }
            current = current.path(path);
        }
        return textOrNull(current);
    }

    private String textAtAny(JsonNode node, String[]... pathOptions) {
        for (String[] path : pathOptions) {
            String value = textAt(node, path);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String cleanQuotedText(JsonNode node) {
        String value = textOrNull(node);
        if (value == null) {
            return null;
        }

        Matcher matcher = WRAPPED_QUOTES_PATTERN.matcher(value);
        if (matcher.matches()) {
            return trimToNull(matcher.group(1));
        }
        return value;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return trimToNull(node.asText());
    }

    private String joinNames(List<String> names) {
        List<String> cleanedNames = names.stream()
                .map(this::trimToNull)
                .filter(name -> name != null)
                .toList();
        if (cleanedNames.isEmpty()) {
            return null;
        }
        if (cleanedNames.size() == 1) {
            return cleanedNames.get(0);
        }
        if (cleanedNames.size() == 2) {
            return cleanedNames.get(0) + " and " + cleanedNames.get(1);
        }

        String prefix = String.join(", ", cleanedNames.subList(0, cleanedNames.size() - 1));
        return prefix + " and " + cleanedNames.get(cleanedNames.size() - 1);
    }

    private AdminPageExtractFieldsResponse normalizeResponse(AdminPageExtractFieldsResponse response) {
        return response.toBuilder()
                .url(trimToNull(response.getUrl()))
                .title(trimToNull(response.getTitle()))
                .extractMode(trimToNull(response.getExtractMode()))
                .generateMode(trimToNull(response.getGenerateMode()))
                .name(trimToNull(response.getName()))
                .englishName(trimToNull(response.getEnglishName()))
                .category(trimToNull(response.getCategory()))
                .heroImage(trimToNull(response.getHeroImage()))
                .difficulty(trimToNull(response.getDifficulty()))
                .abv(normalizeAbv(response.getAbv()))
                .glass(trimToNull(response.getGlass()))
                .garnish(trimToNull(response.getGarnish()))
                .highlight(trimToNull(response.getHighlight()))
                .subtitle(trimToNull(response.getSubtitle()))
                .description(trimToNull(response.getDescription()))
                .story(trimToNull(response.getStory()))
                .flavorTags(normalizeStringList(response.getFlavorTags()))
                .flavorMetrics(normalizeFlavorMetrics(response.getFlavorMetrics()))
                .pairings(normalizeStringList(response.getPairings()))
                .serviceNotes(normalizeStringList(response.getServiceNotes()))
                .ingredients(normalizeIngredients(response.getIngredients()))
                .steps(normalizeSteps(response.getSteps()))
                .fieldSources(normalizeFieldSources(response.getFieldSources()))
                .missingFields(response.getMissingFields() == null ? List.of() : response.getMissingFields())
                .build();
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> normalizeIngredients(
            List<AdminPageExtractFieldsResponse.IngredientItem> ingredients) {
        if (ingredients == null) {
            return List.of();
        }

        return ingredients.stream()
                .map(item -> AdminPageExtractFieldsResponse.IngredientItem.builder()
                        .name(trimToNull(item.getName()))
                        .amount(trimToNull(item.getAmount()))
                        .note(trimToNull(item.getNote()))
                        .build())
                .filter(item -> item.getName() != null || item.getAmount() != null || item.getNote() != null)
                .toList();
    }

    private List<AdminPageExtractFieldsResponse.StepItem> normalizeSteps(
            List<AdminPageExtractFieldsResponse.StepItem> steps) {
        if (steps == null) {
            return List.of();
        }

        List<AdminPageExtractFieldsResponse.StepItem> normalized = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            AdminPageExtractFieldsResponse.StepItem step = steps.get(index);
            String title = trimToNull(step.getTitle());
            String detail = trimToNull(step.getDetail());
            if (title == null && detail == null) {
                continue;
            }
            normalized.add(AdminPageExtractFieldsResponse.StepItem.builder()
                    .title(title)
                    .detail(detail)
                    .build());
        }
        return normalized;
    }

    private List<String> calculateMissingFields(AdminPageExtractFieldsResponse response) {
        List<String> missingFields = new ArrayList<>();
        if (response.getName() == null) {
            missingFields.add("name");
        }
        if (response.getEnglishName() == null) {
            missingFields.add("englishName");
        }
        if (response.getCategory() == null) {
            missingFields.add("category");
        }
        if (response.getHeroImage() == null) {
            missingFields.add("heroImage");
        }
        if (response.getDifficulty() == null) {
            missingFields.add("difficulty");
        }
        if (response.getAbv() == null) {
            missingFields.add("abv");
        }
        if (response.getGlass() == null) {
            missingFields.add("glass");
        }
        if (response.getGarnish() == null) {
            missingFields.add("garnish");
        }
        if (response.getIngredients() == null || response.getIngredients().isEmpty()) {
            missingFields.add("ingredients");
        }
        if (response.getSteps() == null || response.getSteps().isEmpty()) {
            missingFields.add("steps");
        }
        if (response.getHighlight() == null) {
            missingFields.add("highlight");
        }
        if (response.getSubtitle() == null) {
            missingFields.add("subtitle");
        }
        if (response.getDescription() == null) {
            missingFields.add("description");
        }
        if (response.getStory() == null) {
            missingFields.add("story");
        }
        if (response.getFlavorTags() == null || response.getFlavorTags().isEmpty()) {
            missingFields.add("flavorTags");
        }
        if (response.getFlavorMetrics() == null || response.getFlavorMetrics().isEmpty()) {
            missingFields.add("flavorMetrics");
        }
        if (response.getPairings() == null || response.getPairings().isEmpty()) {
            missingFields.add("pairings");
        }
        if (response.getServiceNotes() == null || response.getServiceNotes().isEmpty()) {
            missingFields.add("serviceNotes");
        }
        if (response.getIngredients() != null
                && !response.getIngredients().isEmpty()
                && response.getIngredients().stream().anyMatch(item -> item.getNote() == null)) {
            missingFields.add("ingredients.note");
        }
        return missingFields;
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .toList();
    }

    private Map<String, Integer> normalizeFlavorMetrics(Map<String, Integer> flavorMetrics) {
        if (flavorMetrics == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(flavorMetrics);
    }

    private Map<String, AdminPageExtractFieldsResponse.FieldSource> normalizeFieldSources(
            Map<String, AdminPageExtractFieldsResponse.FieldSource> fieldSources) {
        if (fieldSources == null) {
            return new LinkedHashMap<>();
        }

        Map<String, AdminPageExtractFieldsResponse.FieldSource> normalized = new LinkedHashMap<>();
        fieldSources.forEach((key, value) -> normalized.put(
                key,
                value == null ? null : AdminPageExtractFieldsResponse.FieldSource.builder()
                        .mode(trimToNull(value.getMode()))
                        .source(trimToNull(value.getSource()))
                        .note(trimToNull(value.getNote()))
                        .build()
        ));
        return normalized;
    }

    private String htmlToPlainText(String html) {
        if (html == null) {
            return null;
        }

        String normalizedHtml = HTML_BREAK_PATTERN.matcher(html).replaceAll("\n");
        normalizedHtml = HTML_BLOCK_CLOSE_PATTERN.matcher(normalizedHtml).replaceAll("\n");
        normalizedHtml = HTML_TAG_PATTERN.matcher(normalizedHtml).replaceAll(" ");
        return normalizePlainText(normalizedHtml);
    }

    private String normalizePlainText(String value) {
        if (value == null) {
            return null;
        }

        String unescapedValue = HtmlUtils.htmlUnescape(value).replace('\u00A0', ' ');
        String normalizedWhitespace = WHITESPACE_PATTERN.matcher(unescapedValue).replaceAll(" ");
        String normalizedLineBreaks = normalizedWhitespace.replaceAll("\\s*\\n\\s*", "\n");
        normalizedLineBreaks = MULTI_LINE_BREAK_PATTERN.matcher(normalizedLineBreaks).replaceAll("\n");
        return trimToNull(normalizedLineBreaks);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isBlank() ? null : trimmedValue;
    }

    private String normalizeAbv(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replace("％", "%").replaceAll("\\s+", "");
        if (compact.matches("\\d{1,2}(?:\\.\\d{1,2})?")) {
            return compact + "%";
        }
        if (compact.matches("\\d{1,2}(?:\\.\\d{1,2})?%")) {
            return compact;
        }
        return normalized;
    }

    private AdminPageExtractFieldsResponse.FieldSource extractedSource(String source, String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("extracted")
                .source(source)
                .note(note)
                .build();
    }

    private AdminPageExtractFieldsResponse.FieldSource derivedSource(String source, String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("derived")
                .source(source)
                .note(note)
                .build();
    }

    private AdminPageExtractFieldsResponse.FieldSource missingSource(String note) {
        return AdminPageExtractFieldsResponse.FieldSource.builder()
                .mode("missing")
                .source("parser")
                .note(note)
                .build();
    }
}
