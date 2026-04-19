package com.shakepro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cocktaildb")
public class CocktailDbProperties {

    private boolean enabled = true;
    private String baseUrl = "https://www.thecocktaildb.com";
    private String apiKey = "1";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private Sync sync = new Sync();

    public String getNormalizedBaseUrl() {
        if (baseUrl == null) {
            return "https://www.thecocktaildb.com";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public String getNormalizedApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "1";
        }
        return apiKey.trim();
    }

    @Data
    public static class Sync {
        private int maxItems = 1000;
        private boolean dryRun = false;
        private boolean overwriteImage = false;
        private String ossPrefix = "uploads/materials/cocktaildb";
    }
}
