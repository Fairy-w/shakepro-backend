package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.OssImageUrlBuilder;
import com.shakepro.common.util.ScanFieldUtils;
import com.shakepro.config.BarcodeLookupConfig;
import com.shakepro.dto.request.BarcodeLookupRequest;
import com.shakepro.dto.response.BarcodeLookupResponse;
import com.shakepro.entity.BarcodeProductCache;
import com.shakepro.entity.Material;
import com.shakepro.entity.UserMaterial;
import com.shakepro.repository.BarcodeProductCacheRepository;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.repository.UserMaterialRepository;
import com.shakepro.service.BarcodeLookupService;
import com.shakepro.service.support.MaterialAliasMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarcodeLookupServiceImpl implements BarcodeLookupService {

    private static final String LOOKUP_SOURCE = "scan";
    private static final String LOOKUP_SOURCE_LABEL = "条码识别结果";
    private static final String LOOKUP_SUBTITLE = "已识别品牌与类别，可直接标记是否拥有。";

    private final BarcodeLookupConfig barcodeLookupConfig;
    @Qualifier("barcodeHttpClient")
    private final HttpClient barcodeHttpClient;
    private final ObjectMapper objectMapper;
    private final BarcodeProductCacheRepository barcodeProductCacheRepository;
    private final UserMaterialRepository userMaterialRepository;
    private final MaterialRepository materialRepository;
    private final MaterialAliasMatcher materialAliasMatcher;
    private final OssImageUrlBuilder ossImageUrlBuilder;

    @Override
    @Transactional
    public BarcodeLookupResponse lookup(Long userId, BarcodeLookupRequest request) {
        String barcode = ScanFieldUtils.normalizeBarcode(request.getBarcode());
        if (barcode.length() < 8 || barcode.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "条码长度必须在8到32之间");
        }

        Optional<UserMaterial> existingInventory =
                userMaterialRepository.findByUserIdAndBarcode(userId, barcode);
        if (existingInventory.isPresent()) {
            return toResponse(existingInventory.get());
        }

        BarcodeProductCache cached = barcodeProductCacheRepository.findByBarcode(barcode).orElse(null);
        BarcodeProductCache resolved;
        if (isFreshCache(cached)) {
            resolved = cached;
        } else {
            try {
                resolved = fetchAndCacheFromOpenFoodFacts(barcode).orElse(cached);
            } catch (BusinessException ex) {
                if (cached != null) {
                    log.warn("Barcode lookup fallback to stale cache, barcode={}, reason={}", barcode, ex.getMessage());
                    resolved = cached;
                } else {
                    throw ex;
                }
            }
        }

        if (resolved == null) {
            throw new BusinessException(ErrorCode.BARCODE_NOT_FOUND, "暂未识别到该条码的商品信息");
        }

        return toResponse(resolved, false);
    }

    private Optional<BarcodeProductCache> fetchAndCacheFromOpenFoodFacts(String barcode) {
        if (!barcodeLookupConfig.isEnabled()) {
            return Optional.empty();
        }

        try {
            String url = barcodeLookupConfig.getBaseUrl()
                    + "/api/v2/product/"
                    + URLEncoder.encode(barcode, StandardCharsets.UTF_8)
                    + ".json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("User-Agent", barcodeLookupConfig.getUserAgent())
                    .timeout(Duration.ofMillis(barcodeLookupConfig.getTimeoutMs()))
                    .GET()
                    .build();

            HttpResponse<String> response = barcodeHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.BARCODE_LOOKUP_FAILED, "条码服务响应异常: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("status").asInt(0) != 1 || root.path("product").isMissingNode()) {
                return Optional.empty();
            }

            JsonNode product = root.path("product");
            String name = firstNonBlank(
                    asTextOrNull(product, "product_name_zh"),
                    asTextOrNull(product, "product_name"),
                    asTextOrNull(product, "product_name_en"),
                    asTextOrNull(product, "generic_name")
            );
            if (name == null) {
                return Optional.empty();
            }

            String brand = parseBrand(asTextOrNull(product, "brands"));
            String capacityText = firstNonBlank(asTextOrNull(product, "quantity"), asTextOrNull(product, "product_quantity"));
            List<String> tags = extractTags(product, name, brand);
            MaterialAliasMatcher.MatchResult match = materialAliasMatcher.match(name, brand, tags);
            String categoryId = firstNonBlank(match.categoryId(), materialAliasMatcher.detectCategoryId(name + " " + brand));
            ColorPalette palette = ColorPalette.forCategory(categoryId);

            BarcodeProductCache entity = barcodeProductCacheRepository.findByBarcode(barcode)
                    .orElse(BarcodeProductCache.builder().barcode(barcode).build());
            entity.setProductKey(ScanFieldUtils.slugify(name, barcode));
            entity.setSource("open_food_facts");
            entity.setSourceLabel("Open Food Facts");
            entity.setName(name);
            entity.setSubtitle(LOOKUP_SUBTITLE);
            entity.setBrand(brand);
            entity.setCategoryId(categoryId);
            entity.setCapacityText(capacityText);
            entity.setTagsJson(writeTags(tags));
            entity.setNote(defaultNote(categoryId));
            entity.setBadge(ScanFieldUtils.buildBadge(brand, name));
            entity.setAccentColor(palette.accentColor);
            entity.setSoftColor(palette.softColor);
            entity.setRawPayload(root.toString());
            return Optional.of(barcodeProductCacheRepository.save(entity));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Barcode lookup failed for {}: {}", barcode, e.getMessage(), e);
            throw new BusinessException(ErrorCode.BARCODE_LOOKUP_FAILED, "条码识别失败: " + e.getMessage());
        }
    }

    private boolean isFreshCache(BarcodeProductCache cache) {
        if (cache == null || cache.getUpdatedAt() == null) {
            return false;
        }
        return cache.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(Math.max(1, barcodeLookupConfig.getCacheHours())));
    }

    private BarcodeLookupResponse toResponse(BarcodeProductCache cache, boolean hasItem) {
        List<String> tags = readTags(cache.getTagsJson());
        MaterialAliasMatcher.MatchResult match = materialAliasMatcher.match(cache.getName(), cache.getBrand(), tags);
        Material material = match.materialId() != null
                ? materialRepository.findById(match.materialId()).orElse(null)
                : null;
        String imageUrl = material != null ? material.getImageUrl() : null;

        return BarcodeLookupResponse.builder()
                .source(LOOKUP_SOURCE)
                .imageUrl(imageUrl)
                .imageUrlThumb(ossImageUrlBuilder.toThumbUrl(imageUrl))
                .imageUrlCard(ossImageUrlBuilder.toCardUrl(imageUrl))
                .imageUrlDetail(ossImageUrlBuilder.toDetailUrl(imageUrl))
                .name(cache.getName())
                .brand(cache.getBrand())
                .categoryId(firstNonBlank(cache.getCategoryId(), match.categoryId()))
                .barcode(cache.getBarcode())
                .capacityText(null)
                .remainLevel(null)
                .opened(null)
                .hasItem(hasItem)
                .tags(tags)
                .materialId(match.materialId())
                .build();
    }

    private BarcodeLookupResponse toResponse(UserMaterial materialRecord) {
        String imageUrl = materialRecord.getMaterial() != null ? materialRecord.getMaterial().getImageUrl() : null;
        return BarcodeLookupResponse.builder()
                .source(firstNonBlank(materialRecord.getSource(), LOOKUP_SOURCE))
                .imageUrl(imageUrl)
                .imageUrlThumb(ossImageUrlBuilder.toThumbUrl(imageUrl))
                .imageUrlCard(ossImageUrlBuilder.toCardUrl(imageUrl))
                .imageUrlDetail(ossImageUrlBuilder.toDetailUrl(imageUrl))
                .name(materialRecord.getName())
                .brand(materialRecord.getBrand())
                .categoryId(materialRecord.getCategoryId())
                .barcode(materialRecord.getBarcode())
                .capacityText(null)
                .remainLevel(null)
                .opened(null)
                .hasItem(Boolean.TRUE.equals(materialRecord.getHasItem()))
                .tags(readTags(materialRecord.getTagsJson()))
                .materialId(materialRecord.getMaterial() != null ? materialRecord.getMaterial().getId() : null)
                .build();
    }

    private List<String> extractTags(JsonNode product, String name, String brand) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        JsonNode categoriesTags = product.path("categories_tags");
        if (categoriesTags.isArray()) {
            for (JsonNode tagNode : categoriesTags) {
                String raw = tagNode.asText();
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String clean = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
                clean = clean.replace('-', ' ').replace('_', ' ').trim();
                String translated = switch (clean.toLowerCase(Locale.ROOT)) {
                    case "spirits" -> "烈酒";
                    case "gins" -> "杜松子";
                    case "herbal" -> "草本";
                    default -> clean;
                };
                if (!translated.isBlank()) {
                    tags.add(translated);
                }
                if (tags.size() >= 3) {
                    break;
                }
            }
        }

        String merged = ScanFieldUtils.normalizeAlias(firstNonBlank(name, "") + " " + firstNonBlank(brand, ""));
        if (merged.contains("gin") || merged.contains("杜松子") || merged.contains("金酒")) {
            tags.add("杜松子");
        }
        if (merged.contains("herb") || merged.contains("草本")) {
            tags.add("草本");
        }
        if (merged.contains("cucumber") || merged.contains("黄瓜")) {
            tags.add("黄瓜");
        }

        if (tags.isEmpty()) {
            tags.add("调酒");
        }

        return tags.stream().limit(3).toList();
    }

    private String defaultNote(String categoryId) {
        if ("spirit".equalsIgnoreCase(categoryId)) {
            return "适合用于 Gin Tonic、Southside 等清爽型配方。";
        }
        if ("mixer".equalsIgnoreCase(categoryId)) {
            return "适合作为鸡尾酒配料与基酒组合使用。";
        }
        return "可加入你的材料库，用于后续配方推荐。";
    }

    private String parseBrand(String brands) {
        String normalized = ScanFieldUtils.trimToNull(brands);
        if (normalized == null) {
            return null;
        }
        String[] parts = normalized.split(",");
        return parts.length == 0 ? normalized : parts[0].trim();
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? List.of() : tags);
        } catch (Exception e) {
            log.warn("Serialize tags failed: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> readTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(tagsJson);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> tags = new ArrayList<>();
            for (JsonNode item : node) {
                String text = ScanFieldUtils.trimToNull(item.asText());
                if (text != null) {
                    tags.add(text);
                }
            }
            return tags;
        } catch (Exception e) {
            log.warn("Parse tags json failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String asTextOrNull(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        return ScanFieldUtils.trimToNull(child.asText());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = ScanFieldUtils.trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static final class ColorPalette {
        private final String accentColor;
        private final String softColor;

        private ColorPalette(String accentColor, String softColor) {
            this.accentColor = accentColor;
            this.softColor = softColor;
        }

        private static ColorPalette forCategory(String categoryId) {
            String category = categoryId == null ? "" : categoryId.toLowerCase(Locale.ROOT);
            return switch (category) {
                case "spirit" -> new ColorPalette("#1B596E", "#DDF5F1");
                case "fruity" -> new ColorPalette("#C75B39", "#FCEBDD");
                case "mixer" -> new ColorPalette("#2F5D9F", "#DFE9FA");
                case "sweetener" -> new ColorPalette("#92733A", "#F7EEDC");
                case "herb" -> new ColorPalette("#3A7A43", "#E1F4E4");
                default -> new ColorPalette("#5A6170", "#EEF1F5");
            };
        }
    }
}
