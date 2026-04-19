package com.shakepro.common.util;

import java.util.Locale;

public final class ScanFieldUtils {

    private ScanFieldUtils() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return "";
        }
        return barcode.replaceAll("[^0-9]", "");
    }

    public static String normalizeAlias(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[\\s\\-_'`]+", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+", "");
    }

    public static String slugify(String value, String fallback) {
        if (value != null) {
            String slug = value.toLowerCase(Locale.ROOT)
                    .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+", "-")
                    .replaceAll("-{2,}", "-")
                    .replaceAll("(^-|-$)", "");
            if (!slug.isBlank()) {
                return slug;
            }
        }
        String normalized = normalizeAlias(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String fallbackNormalized = normalizeAlias(fallback);
        return fallbackNormalized.isEmpty() ? "item" : fallbackNormalized;
    }

    public static String buildBadge(String brand, String name) {
        String seed = trimToNull(brand);
        if (seed == null) {
            seed = trimToNull(name);
        }
        if (seed == null) {
            return "SP";
        }

        String lettersOnly = seed.replaceAll("[^A-Za-z0-9\\p{IsIdeographic}]", "");
        if (lettersOnly.isEmpty()) {
            return "SP";
        }

        if (lettersOnly.matches(".*[\\p{IsIdeographic}].*")) {
            return lettersOnly.substring(0, Math.min(2, lettersOnly.length()));
        }

        return lettersOnly.substring(0, Math.min(2, lettersOnly.length())).toUpperCase(Locale.ROOT);
    }
}
