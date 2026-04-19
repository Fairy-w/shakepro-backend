package com.shakepro.service.support;

import com.shakepro.common.util.ScanFieldUtils;
import com.shakepro.entity.Material;
import com.shakepro.entity.MaterialAlias;
import com.shakepro.repository.MaterialAliasRepository;
import com.shakepro.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class MaterialAliasMatcher {

    private final MaterialAliasRepository materialAliasRepository;
    private final MaterialRepository materialRepository;

    public MatchResult match(String name, String brand, List<String> tags) {
        Set<String> candidates = buildAliasCandidates(name, brand, tags);
        if (!candidates.isEmpty()) {
            List<MaterialAlias> aliases = materialAliasRepository.findByAliasNormalizedInOrderByPriorityAscIdAsc(candidates);
            if (!aliases.isEmpty()) {
                Material material = aliases.get(0).getMaterial();
                return new MatchResult(material.getId(), material.getCategory());
            }
        }

        String merged = String.join(" ", nonNull(name), nonNull(brand), String.join(" ", tags == null ? List.of() : tags));
        Optional<Material> fallback = fallbackMaterial(merged);
        if (fallback.isPresent()) {
            Material material = fallback.get();
            return new MatchResult(material.getId(), material.getCategory());
        }

        return new MatchResult(null, detectCategoryId(merged));
    }

    public String detectCategoryId(String text) {
        String normalized = ScanFieldUtils.normalizeAlias(text);
        if (containsAny(normalized, List.of("gin", "vodka", "rum", "whisky", "whiskey", "tequila", "brandy", "liqueur",
                "金酒", "伏特加", "朗姆", "威士忌", "龙舌兰", "白兰地", "利口酒"))) {
            return "spirit";
        }
        if (containsAny(normalized, List.of("juice", "orange", "pineapple", "cranberry", "fruit", "果汁", "柠檬", "葡萄柚", "菠萝", "蔓越莓"))) {
            return "fruity";
        }
        if (containsAny(normalized, List.of("syrup", "honey", "糖浆", "蜂蜜", "甜味"))) {
            return "sweetener";
        }
        if (containsAny(normalized, List.of("soda", "tonic", "cola", "gingerbeer", "苏打", "汤力", "可乐"))) {
            return "mixer";
        }
        if (containsAny(normalized, List.of("mint", "basil", "薄荷", "罗勒"))) {
            return "herb";
        }
        return "other";
    }

    private Optional<Material> fallbackMaterial(String merged) {
        String normalized = ScanFieldUtils.normalizeAlias(merged);
        Map<String, String> keywordToMaterialName = new LinkedHashMap<>();
        keywordToMaterialName.put("gin", "金酒");
        keywordToMaterialName.put("vodka", "伏特加");
        keywordToMaterialName.put("rum", "朗姆");
        keywordToMaterialName.put("whisky", "威士忌");
        keywordToMaterialName.put("whiskey", "威士忌");
        keywordToMaterialName.put("tequila", "龙舌兰酒");
        keywordToMaterialName.put("金酒", "金酒");
        keywordToMaterialName.put("伏特加", "伏特加");
        keywordToMaterialName.put("朗姆", "朗姆");
        keywordToMaterialName.put("威士忌", "威士忌");
        keywordToMaterialName.put("龙舌兰", "龙舌兰酒");

        for (Map.Entry<String, String> entry : keywordToMaterialName.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                List<Material> materials = materialRepository.findByNameContainingIgnoreCase(entry.getValue());
                if (!materials.isEmpty()) {
                    return Optional.of(materials.get(0));
                }
            }
        }
        return Optional.empty();
    }

    private Set<String> buildAliasCandidates(String name, String brand, List<String> tags) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String text : List.of(nonNull(name), nonNull(brand))) {
            String normalizedText = ScanFieldUtils.normalizeAlias(text);
            if (!normalizedText.isEmpty()) {
                candidates.add(normalizedText);
            }
            for (String token : splitToTokens(text)) {
                String normalizedToken = ScanFieldUtils.normalizeAlias(token);
                if (normalizedToken.length() >= 2) {
                    candidates.add(normalizedToken);
                }
            }
        }

        if (tags != null) {
            for (String tag : tags) {
                String normalizedTag = ScanFieldUtils.normalizeAlias(tag);
                if (normalizedTag.length() >= 2) {
                    candidates.add(normalizedTag);
                }
            }
        }

        String merged = ScanFieldUtils.normalizeAlias(String.join(" ", nonNull(name), nonNull(brand)));
        if (merged.contains("gin") || merged.contains("金酒") || merged.contains("杜松子")) {
            candidates.add("gin");
        }
        if (merged.contains("vodka") || merged.contains("伏特加")) {
            candidates.add("vodka");
        }
        if (merged.contains("rum") || merged.contains("朗姆")) {
            candidates.add("rum");
        }
        if (merged.contains("whisky") || merged.contains("whiskey") || merged.contains("威士忌")) {
            candidates.add("whisky");
        }
        if (merged.contains("tequila") || merged.contains("龙舌兰")) {
            candidates.add("tequila");
        }

        return candidates;
    }

    private List<String> splitToTokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    public record MatchResult(Long materialId, String categoryId) {
    }
}
