package com.shakepro.common.util;

import com.shakepro.config.OssConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OssImageUrlBuilder {

    private static final String STYLE_QUERY_PREFIX = "x-oss-process=style/";

    private final OssConfig ossConfig;

    public String toThumbUrl(String originalUrl) {
        return withStyle(originalUrl, ossConfig.getThumbStyleName());
    }

    public String toCardUrl(String originalUrl) {
        return withStyle(originalUrl, ossConfig.getCardStyleName());
    }

    public String toDetailUrl(String originalUrl) {
        return withStyle(originalUrl, ossConfig.getDetailStyleName());
    }

    public String withStyle(String originalUrl, String styleName) {
        String normalizedUrl = normalize(originalUrl);
        String normalizedStyleName = normalize(styleName);
        if (normalizedUrl == null || normalizedStyleName == null) {
            return normalizedUrl;
        }

        String publicBaseUrl = normalize(ossConfig.getNormalizedPublicBaseUrl());
        if (publicBaseUrl == null || !normalizedUrl.startsWith(publicBaseUrl + "/")) {
            return normalizedUrl;
        }
        if (normalizedUrl.contains("x-oss-process=")) {
            return normalizedUrl;
        }

        return normalizedUrl + (normalizedUrl.contains("?") ? "&" : "?") + STYLE_QUERY_PREFIX + normalizedStyleName;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
