package com.shakepro.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssConfig {

    private String type;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String publicBaseUrl;
    private int presignExpireSeconds = 3600;
    private ImageStyle imageStyle = new ImageStyle();

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(
                normalizeEndpoint(endpoint),
                sanitize(accessKey),
                sanitize(secretKey)
        );
    }

    public String getNormalizedBucket() {
        return sanitize(bucket);
    }

    public String getNormalizedPublicBaseUrl() {
        String normalized = sanitize(publicBaseUrl);
        if (normalized == null) {
            return null;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    public int getEffectivePresignExpireSeconds() {
        return presignExpireSeconds > 0 ? presignExpireSeconds : 3600;
    }

    public String getThumbStyleName() {
        return sanitize(imageStyle != null ? imageStyle.getThumb() : null);
    }

    public String getCardStyleName() {
        return sanitize(imageStyle != null ? imageStyle.getCard() : null);
    }

    public String getDetailStyleName() {
        return sanitize(imageStyle != null ? imageStyle.getDetail() : null);
    }

    private String normalizeEndpoint(String rawEndpoint) {
        String normalized = sanitize(rawEndpoint);
        if (normalized == null) {
            return null;
        }
        if (normalized.contains("://s3.oss-")) {
            return normalized.replace("://s3.oss-", "://oss-");
        }
        return normalized;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    @Data
    public static class ImageStyle {
        private String thumb = "thumb";
        private String card = "card";
        private String detail = "detail";
    }

}
