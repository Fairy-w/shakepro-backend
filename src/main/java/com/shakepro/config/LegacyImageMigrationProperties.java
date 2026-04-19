package com.shakepro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "migration.legacy-images")
public class LegacyImageMigrationProperties {

    private boolean enabled = false;
    private String mode = "inventory";
    private boolean dryRun = false;
    private boolean verifyAfterMigrate = true;
    private boolean includeFileRecordUrl = false;

    private int batchSize = 200;
    private int sampleSize = 50;
    private int maxRetries = 3;
    private int retryBackoffMillis = 800;
    private long maxImageSizeBytes = 10L * 1024 * 1024;

    private int connectTimeoutSeconds = 8;
    private int readTimeoutSeconds = 20;

    private String ossPrefix = "uploads/legacy";
    private String reportDir = "migration-reports";
}
