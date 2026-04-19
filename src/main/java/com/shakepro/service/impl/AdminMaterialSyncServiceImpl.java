package com.shakepro.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.config.CocktailDbProperties;
import com.shakepro.config.OssConfig;
import com.shakepro.dto.request.admin.AdminMaterialSyncRequest;
import com.shakepro.dto.response.admin.AdminMaterialSyncResponse;
import com.shakepro.entity.Material;
import com.shakepro.repository.MaterialRepository;
import com.shakepro.service.AdminMaterialSyncService;
import com.shakepro.service.support.CocktailDbIngredientClient;
import com.shakepro.service.support.IngredientDictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMaterialSyncServiceImpl implements AdminMaterialSyncService {

    private static final String SOURCE = "thecocktaildb";
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    private final MaterialRepository materialRepository;
    private final IngredientDictionaryService ingredientDictionaryService;
    private final CocktailDbIngredientClient cocktailDbIngredientClient;
    private final CocktailDbProperties cocktailDbProperties;
    private final OSS ossClient;
    private final OssConfig ossConfig;

    @Override
    @Transactional
    public AdminMaterialSyncResponse syncFromCocktailDb(AdminMaterialSyncRequest request) {
        if (isBlank(ossConfig.getNormalizedBucket()) || isBlank(ossConfig.getNormalizedPublicBaseUrl())) {
            throw new BusinessException(ErrorCode.OSS_ERROR, "OSS配置不完整，缺少bucket或publicBaseUrl");
        }

        SyncOptions options = resolveOptions(request);
        List<String> ingredients = cocktailDbIngredientClient.listIngredients();
        int totalFetched = ingredients.size();
        int max = options.maxItems() > 0 ? Math.min(options.maxItems(), ingredients.size()) : ingredients.size();

        int processed = 0;
        int matchedByDictionary = 0;
        int created = 0;
        int updated = 0;
        int skippedNoDictionary = 0;
        int skippedImageExists = 0;
        int failed = 0;

        for (int i = 0; i < max; i++) {
            String englishName = ingredients.get(i);
            processed++;

            IngredientDictionaryService.DictionaryItem dictItem = ingredientDictionaryService.findByEnglish(englishName);
            if (dictItem == null) {
                skippedNoDictionary++;
                continue;
            }
            matchedByDictionary++;

            try {
                Optional<Material> existingByEn = materialRepository.findFirstByNameEnIgnoreCase(dictItem.english());
                Optional<Material> existingByZh = materialRepository.findFirstByNameIgnoreCase(dictItem.chinese());
                Optional<Material> existingByEnglishAsName = materialRepository.findFirstByNameIgnoreCase(dictItem.english());
                Material material = existingByEn.or(() -> existingByZh).or(() -> existingByEnglishAsName).orElse(null);

                boolean isCreate = material == null;
                if (material == null) {
                    material = Material.builder().build();
                }

                boolean shouldUploadImage = options.overwriteImage() || isBlank(material.getImageUrl());
                String targetImageUrl = material.getImageUrl();
                if (shouldUploadImage) {
                    if (options.dryRun()) {
                        targetImageUrl = buildSourceImageUrl(dictItem.english());
                    } else {
                        targetImageUrl = mirrorIngredientImageToOss(dictItem.english(), options.ossPrefix());
                    }
                } else {
                    skippedImageExists++;
                }

                if (!options.dryRun()) {
                    material.setName(dictItem.chinese());
                    material.setCategory(dictItem.category());
                    material.setNameEn(dictItem.english());
                    material.setImageUrl(targetImageUrl);
                    material.setSource(SOURCE);
                    material.setSourceId(IngredientDictionaryService.normalizeKey(dictItem.english()));
                    materialRepository.save(material);
                }

                if (isCreate) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("Sync ingredient failed. english={}, reason={}", englishName, ex.getMessage());
            }
        }

        return AdminMaterialSyncResponse.builder()
                .totalFetched(totalFetched)
                .processed(processed)
                .matchedByDictionary(matchedByDictionary)
                .created(created)
                .updated(updated)
                .skippedNoDictionary(skippedNoDictionary)
                .skippedImageExists(skippedImageExists)
                .failed(failed)
                .dryRun(options.dryRun())
                .build();
    }

    private SyncOptions resolveOptions(AdminMaterialSyncRequest request) {
        CocktailDbProperties.Sync sync = cocktailDbProperties.getSync();
        int maxItems = request != null && request.getMaxItems() != null ? request.getMaxItems() : sync.getMaxItems();
        boolean dryRun = request != null && request.getDryRun() != null ? request.getDryRun() : sync.isDryRun();
        boolean overwriteImage = request != null && request.getOverwriteImage() != null ? request.getOverwriteImage() : sync.isOverwriteImage();
        String ossPrefix = normalizePrefix(sync.getOssPrefix());
        return new SyncOptions(maxItems, dryRun, overwriteImage, ossPrefix);
    }

    private String mirrorIngredientImageToOss(String englishName, String ossPrefix) throws Exception {
        String sourceImageUrl = buildSourceImageUrl(englishName);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cocktailDbProperties.getConnectTimeoutMs()))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceImageUrl))
                .timeout(Duration.ofMillis(cocktailDbProperties.getReadTimeoutMs()))
                .header("Accept", "image/*")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("download failed http=" + response.statusCode());
        }
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IllegalStateException("download image empty");
        }
        if (body.length > MAX_IMAGE_SIZE) {
            throw new IllegalStateException("image too large >10MB");
        }

        String contentType = normalizeContentType(response.headers().firstValue("Content-Type").orElse("image/png"));
        if (!contentType.startsWith("image/")) {
            throw new IllegalStateException("content type is not image: " + contentType);
        }

        String objectKey = buildObjectKey(ossPrefix, englishName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(body.length);
        ossClient.putObject(
                ossConfig.getNormalizedBucket(),
                objectKey,
                new ByteArrayInputStream(body),
                metadata
        );
        return ossConfig.getNormalizedPublicBaseUrl() + "/" + objectKey;
    }

    private String buildSourceImageUrl(String englishName) {
        String encoded = URLEncoder.encode(englishName.trim(), StandardCharsets.UTF_8).replace("+", "%20");
        return cocktailDbProperties.getNormalizedBaseUrl() + "/images/ingredients/" + encoded + ".png";
    }

    private String buildObjectKey(String prefix, String englishName) {
        String slug = IngredientDictionaryService.normalizeKey(englishName)
                .replace(' ', '-')
                .replaceAll("[^a-z0-9\\-]", "");
        if (slug.isEmpty()) {
            slug = "ingredient-" + Integer.toHexString(englishName.hashCode());
        }
        return prefix + "/" + slug + ".png";
    }

    private String normalizePrefix(String value) {
        String prefix = isBlank(value) ? "uploads/materials/cocktaildb" : value.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String normalizeContentType(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(';');
        return separator >= 0 ? normalized.substring(0, separator).trim() : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record SyncOptions(int maxItems, boolean dryRun, boolean overwriteImage, String ossPrefix) {
    }
}
