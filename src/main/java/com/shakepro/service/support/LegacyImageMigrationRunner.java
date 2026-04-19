package com.shakepro.service.support;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.shakepro.config.LegacyImageMigrationProperties;
import com.shakepro.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "migration.legacy-images", name = "enabled", havingValue = "true")
public class LegacyImageMigrationRunner implements ApplicationRunner {

    private static final DateTimeFormatter REPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Map<String, String> CONTENT_TYPE_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "image/svg+xml", ".svg",
            "image/avif", ".avif"
    );

    private static final String MODE_INVENTORY = "inventory";
    private static final String MODE_MIGRATE = "migrate";
    private static final String MODE_VERIFY = "verify";

    private final LegacyImageMigrationProperties properties;
    private final OssConfig ossConfig;
    private final OSS ossClient;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String mode = normalizeMode(properties.getMode());
        validateConfiguration();

        log.info("Legacy image migration started. mode={}, dryRun={}, batchSize={}, maxRetries={}",
                mode, properties.isDryRun(), properties.getBatchSize(), properties.getMaxRetries());

        InventorySummary inventorySummary = runInventory();
        printInventorySummary(inventorySummary);

        if (MODE_INVENTORY.equals(mode)) {
            log.info("Inventory-only mode completed.");
            return;
        }

        if (MODE_MIGRATE.equals(mode)) {
            MigrationSummary migrationSummary = runMigration(inventorySummary);
            printMigrationSummary(migrationSummary);

            if (properties.isVerifyAfterMigrate()) {
                VerificationSummary verificationSummary = runVerification();
                printVerificationSummary(verificationSummary);
            }
            return;
        }

        VerificationSummary verificationSummary = runVerification();
        printVerificationSummary(verificationSummary);
    }

    private void validateConfiguration() {
        if (isBlank(ossConfig.getNormalizedBucket())) {
            throw new IllegalStateException("OSS bucket is required for legacy image migration");
        }
        if (isBlank(ossConfig.getNormalizedPublicBaseUrl())) {
            throw new IllegalStateException("OSS public-base-url is required for legacy image migration");
        }
        if (properties.getBatchSize() <= 0) {
            properties.setBatchSize(200);
        }
        if (properties.getMaxRetries() <= 0) {
            properties.setMaxRetries(3);
        }
        if (properties.getSampleSize() <= 0) {
            properties.setSampleSize(50);
        }
        if (properties.getRetryBackoffMillis() < 0) {
            properties.setRetryBackoffMillis(0);
        }
        if (properties.getMaxImageSizeBytes() <= 0) {
            properties.setMaxImageSizeBytes(10L * 1024 * 1024);
        }
    }

    private InventorySummary runInventory() {
        InventorySummary summary = new InventorySummary();
        for (TargetColumn target : getTargets()) {
            TargetInventory inventory = new TargetInventory(target);
            forEachRow(target, row -> {
                inventory.totalWithValue++;
                String normalizedUrl = normalizeUrl(row.url());
                if (normalizedUrl == null || !isHttpUrl(normalizedUrl)) {
                    return;
                }
                inventory.httpUrlCount++;
                if (isManagedOssUrl(normalizedUrl)) {
                    inventory.alreadyOssCount++;
                } else {
                    inventory.pendingMigrationCount++;
                }
            });
            summary.targetInventories.put(target.key(), inventory);
        }
        return summary;
    }

    private MigrationSummary runMigration(InventorySummary inventorySummary) throws Exception {
        Path reportDir = Files.createDirectories(Path.of(properties.getReportDir()));
        String timestamp = LocalDateTime.now().format(REPORT_TIME_FORMATTER);
        Path reportCsvPath = reportDir.resolve("legacy-image-migration-" + timestamp + ".csv");
        Path rollbackSqlPath = reportDir.resolve("legacy-image-rollback-" + timestamp + ".sql");

        MigrationSummary summary = new MigrationSummary();
        summary.reportCsvPath = reportCsvPath;
        summary.rollbackSqlPath = rollbackSqlPath;
        summary.inventorySummary = inventorySummary;

        Map<String, UploadResult> uploadCacheByUrl = new LinkedHashMap<>();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try (BufferedWriter reportWriter = Files.newBufferedWriter(reportCsvPath, StandardCharsets.UTF_8);
             BufferedWriter rollbackWriter = Files.newBufferedWriter(rollbackSqlPath, StandardCharsets.UTF_8)) {
            writeReportHeader(reportWriter);
            writeRollbackHeader(rollbackWriter);

            for (TargetColumn target : getTargets()) {
                TargetMigration targetMigration = new TargetMigration(target);
                forEachRow(target, row -> {
                    targetMigration.scanned++;
                    String sourceUrl = normalizeUrl(row.url());
                    if (sourceUrl == null || !isHttpUrl(sourceUrl)) {
                        targetMigration.skippedNonHttp++;
                        return;
                    }
                    if (isManagedOssUrl(sourceUrl)) {
                        targetMigration.skippedAlreadyOss++;
                        return;
                    }

                    targetMigration.pending++;
                    try {
                        UploadResult uploadResult = uploadCacheByUrl.get(sourceUrl);
                        boolean reusedUpload = uploadResult != null;

                        if (uploadResult == null) {
                            uploadResult = transferWithRetry(sourceUrl, target, row.id(), httpClient, targetMigration);
                            uploadCacheByUrl.put(sourceUrl, uploadResult);
                        } else {
                            targetMigration.reusedUploadCount++;
                        }

                        String targetUrl = uploadResult.publicUrl();
                        if (properties.isDryRun()) {
                            targetMigration.dryRunCount++;
                            writeReportRow(reportWriter, target, row.id(), sourceUrl, targetUrl, "DRY_RUN", "", reusedUpload);
                            return;
                        }

                        int updated = updateUrl(target, row.id(), targetUrl);
                        if (updated <= 0) {
                            targetMigration.failed++;
                            writeReportRow(reportWriter, target, row.id(), sourceUrl, targetUrl, "FAILED", "update affected 0 row", reusedUpload);
                            return;
                        }

                        targetMigration.updated++;
                        writeReportRow(reportWriter, target, row.id(), sourceUrl, targetUrl, "UPDATED", "", reusedUpload);
                        writeRollbackSql(rollbackWriter, target, row.id(), sourceUrl, targetUrl);
                    } catch (Exception ex) {
                        targetMigration.failed++;
                        String error = compactError(ex);
                        log.warn("Legacy image migrate failed: {}#{}, field={}, url={}, reason={}",
                                target.tableName(), row.id(), target.columnName(), sourceUrl, error);
                        try {
                            writeReportRow(reportWriter, target, row.id(), sourceUrl, "", "FAILED", error, false);
                        } catch (IOException ioEx) {
                            log.error("Failed to write migration report row: {}", compactError(ioEx), ioEx);
                        }
                    }
                });
                summary.targetMigrations.put(target.key(), targetMigration);
            }
        }

        return summary;
    }

    private VerificationSummary runVerification() {
        List<String> ossUrls = collectManagedOssUrls();
        VerificationSummary summary = new VerificationSummary();
        summary.totalManagedUrlCount = ossUrls.size();
        if (ossUrls.isEmpty()) {
            return summary;
        }

        int sampleSize = Math.min(properties.getSampleSize(), ossUrls.size());
        Collections.shuffle(ossUrls, new Random());
        List<String> sampleUrls = ossUrls.subList(0, sampleSize);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        for (String url : sampleUrls) {
            summary.sampled++;
            try {
                if (isAccessibleUrl(url, httpClient)) {
                    summary.success++;
                } else {
                    summary.failed++;
                    summary.failedUrls.add(url);
                }
            } catch (Exception ex) {
                summary.failed++;
                summary.failedUrls.add(url + " | " + compactError(ex));
            }
        }
        return summary;
    }

    private UploadResult transferWithRetry(
            String sourceUrl,
            TargetColumn target,
            long rowId,
            HttpClient httpClient,
            TargetMigration targetMigration
    ) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                DownloadedImage image = downloadImage(sourceUrl, httpClient);
                String objectKey = buildObjectKey(target, rowId, image);
                String publicUrl = uploadToOss(objectKey, image);
                return new UploadResult(objectKey, publicUrl, image.contentType(), image.size());
            } catch (RecoverableMigrationException ex) {
                last = ex;
                if (attempt >= properties.getMaxRetries()) {
                    break;
                }
                targetMigration.retryCount++;
                sleepQuietly((long) properties.getRetryBackoffMillis() * attempt);
            }
        }
        throw last == null ? new IllegalStateException("transfer failed with unknown error") : last;
    }

    private DownloadedImage downloadImage(String sourceUrl, HttpClient httpClient) throws Exception {
        URI uri = URI.create(sourceUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("Accept", "image/*")
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        int statusCode = response.statusCode();
        if (statusCode == 429 || statusCode >= 500) {
            throw new RecoverableMigrationException("download retryable status: " + statusCode);
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("download failed status: " + statusCode);
        }

        String contentType = normalizeContentType(response.headers());
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("response is not image, contentType=" + contentType);
        }

        byte[] body = response.body();
        if (body.length > properties.getMaxImageSizeBytes()) {
            throw new IllegalArgumentException("image too large: " + body.length + " bytes");
        }
        return new DownloadedImage(body, contentType, body.length, uri);
    }

    private String uploadToOss(String objectKey, DownloadedImage image) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(image.contentType());
            metadata.setContentLength(image.size());

            ossClient.putObject(
                    ossConfig.getNormalizedBucket(),
                    objectKey,
                    new ByteArrayInputStream(image.bytes()),
                    metadata
            );
            return ossConfig.getNormalizedPublicBaseUrl() + "/" + objectKey;
        } catch (OSSException ex) {
            if (isRetryableOssException(ex)) {
                throw new RecoverableMigrationException("oss upload retryable error: " + compactError(ex), ex);
            }
            throw ex;
        }
    }

    private boolean isRetryableOssException(OSSException ex) {
        String errorCode = Optional.ofNullable(ex.getErrorCode()).orElse("").toLowerCase(Locale.ROOT);
        String message = Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase(Locale.ROOT);

        if (errorCode.contains("throttle")
                || errorCode.contains("timeout")
                || errorCode.contains("serviceunavailable")
                || errorCode.contains("slowdown")) {
            return true;
        }

        return message.contains("429")
                || message.contains("500")
                || message.contains("502")
                || message.contains("503")
                || message.contains("504");
    }

    private String buildObjectKey(TargetColumn target, long rowId, DownloadedImage image) throws Exception {
        String prefix = trimSlashes(properties.getOssPrefix());
        String hash = sha256Hex(image.bytes());
        String shortHash = hash.substring(0, Math.min(hash.length(), 24));
        String extension = detectExtension(image.sourceUri(), image.contentType());
        return prefix + "/" + target.tableName() + "/" + rowId + "-" + shortHash + extension;
    }

    private int updateUrl(TargetColumn target, long id, String targetUrl) {
        String sql = "UPDATE " + target.tableName() + " SET " + target.columnName() + " = ? WHERE id = ?";
        return jdbcTemplate.update(sql, targetUrl, id);
    }

    private void forEachRow(TargetColumn target, RowConsumer consumer) {
        long lastId = 0L;
        int batchSize = properties.getBatchSize();
        while (true) {
            long currentLastId = lastId;
            String sql = "SELECT id, " + target.columnName() + " AS image_url FROM " + target.tableName()
                    + " WHERE id > ? AND " + target.columnName() + " IS NOT NULL AND TRIM(" + target.columnName() + ") <> ''"
                    + " ORDER BY id ASC LIMIT ?";
            List<ImageRow> batch = jdbcTemplate.query(
                    sql,
                    ps -> {
                        ps.setLong(1, currentLastId);
                        ps.setInt(2, batchSize);
                    },
                    (rs, rowNum) -> new ImageRow(rs.getLong("id"), rs.getString("image_url"))
            );
            if (batch.isEmpty()) {
                break;
            }
            for (ImageRow row : batch) {
                consumer.accept(row);
                lastId = row.id();
            }
        }
    }

    private List<String> collectManagedOssUrls() {
        List<String> result = new ArrayList<>();
        for (TargetColumn target : getTargets()) {
            forEachRow(target, row -> {
                String normalized = normalizeUrl(row.url());
                if (normalized != null && isManagedOssUrl(normalized)) {
                    result.add(normalized);
                }
            });
        }
        return result;
    }

    private boolean isAccessibleUrl(String url, HttpClient httpClient) throws Exception {
        URI uri = URI.create(url);

        HttpRequest headRequest = HttpRequest.newBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .build();
        HttpResponse<Void> headResponse = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
        if (headResponse.statusCode() >= 200 && headResponse.statusCode() < 400) {
            return true;
        }
        if (headResponse.statusCode() >= 500 || headResponse.statusCode() == 429) {
            throw new RecoverableMigrationException("verify head retryable status: " + headResponse.statusCode());
        }

        HttpRequest getRequest = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("Range", "bytes=0-0")
                .build();
        HttpResponse<Void> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.discarding());
        return getResponse.statusCode() >= 200 && getResponse.statusCode() < 400;
    }

    private List<TargetColumn> getTargets() {
        List<TargetColumn> targets = new ArrayList<>();
        targets.add(new TargetColumn("cocktails", "hero_image"));
        targets.add(new TargetColumn("cocktails", "image_url"));
        targets.add(new TargetColumn("users", "avatar_url"));
        if (properties.isIncludeFileRecordUrl()) {
            targets.add(new TargetColumn("files", "url"));
        }
        return targets;
    }

    private String normalizeMode(String rawMode) {
        String mode = Optional.ofNullable(rawMode).orElse(MODE_INVENTORY).trim().toLowerCase(Locale.ROOT);
        if (MODE_INVENTORY.equals(mode) || MODE_MIGRATE.equals(mode) || MODE_VERIFY.equals(mode)) {
            return mode;
        }
        throw new IllegalArgumentException("Unsupported mode: " + rawMode + ", expected inventory|migrate|verify");
    }

    private boolean isManagedOssUrl(String url) {
        String publicBaseUrl = ossConfig.getNormalizedPublicBaseUrl();
        if (isBlank(publicBaseUrl)) {
            return false;
        }
        return url.equals(publicBaseUrl) || url.startsWith(publicBaseUrl + "/");
    }

    private boolean isHttpUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.endsWith("?")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String normalizeContentType(HttpHeaders headers) {
        String raw = headers.firstValue("Content-Type").orElse("application/octet-stream");
        int separator = raw.indexOf(';');
        String normalized = separator >= 0 ? raw.substring(0, separator) : raw;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String detectExtension(URI sourceUri, String contentType) {
        String fromContentType = CONTENT_TYPE_EXTENSION.get(contentType);
        if (fromContentType != null) {
            return fromContentType;
        }

        String path = sourceUri.getPath() == null ? "" : sourceUri.getPath();
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < path.length() - 1) {
            String ext = path.substring(dotIndex).toLowerCase(Locale.ROOT);
            if (ext.length() <= 8) {
                return ext;
            }
        }
        return ".img";
    }

    private String sha256Hex(byte[] input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(input);
        StringBuilder sb = new StringBuilder(hashed.length * 2);
        for (byte b : hashed) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }

    private String trimSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "uploads/legacy" : normalized;
    }

    private void writeReportHeader(BufferedWriter writer) throws IOException {
        writer.write("table,id,column,source_url,target_url,status,error,reused_upload");
        writer.newLine();
    }

    private void writeReportRow(
            BufferedWriter writer,
            TargetColumn target,
            long id,
            String sourceUrl,
            String targetUrl,
            String status,
            String error,
            boolean reusedUpload
    ) throws IOException {
        writer.write(csv(target.tableName()));
        writer.write(',');
        writer.write(Long.toString(id));
        writer.write(',');
        writer.write(csv(target.columnName()));
        writer.write(',');
        writer.write(csv(sourceUrl));
        writer.write(',');
        writer.write(csv(targetUrl));
        writer.write(',');
        writer.write(csv(status));
        writer.write(',');
        writer.write(csv(error));
        writer.write(',');
        writer.write(Boolean.toString(reusedUpload));
        writer.newLine();
        writer.flush();
    }

    private void writeRollbackHeader(BufferedWriter writer) throws IOException {
        writer.write("-- Auto generated rollback SQL for legacy image migration");
        writer.newLine();
        writer.write("-- Generated at " + LocalDateTime.now());
        writer.newLine();
    }

    private void writeRollbackSql(
            BufferedWriter writer,
            TargetColumn target,
            long id,
            String originalUrl,
            String migratedUrl
    ) throws IOException {
        String sql = "UPDATE " + target.tableName() + " SET " + target.columnName() + " = '"
                + escapeSql(originalUrl) + "' WHERE id = " + id + " AND " + target.columnName()
                + " = '" + escapeSql(migratedUrl) + "';";
        writer.write(sql);
        writer.newLine();
        writer.flush();
    }

    private String escapeSql(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("'", "''");
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String compactError(Throwable throwable) {
        String message = throwable.getMessage();
        if (isBlank(message)) {
            return throwable.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void printInventorySummary(InventorySummary summary) {
        log.info("Inventory summary:");
        for (TargetInventory inventory : summary.targetInventories.values()) {
            log.info("  {}.{} -> totalWithValue={}, httpUrl={}, alreadyOss={}, pendingMigration={}",
                    inventory.target.tableName(),
                    inventory.target.columnName(),
                    inventory.totalWithValue,
                    inventory.httpUrlCount,
                    inventory.alreadyOssCount,
                    inventory.pendingMigrationCount);
        }
    }

    private void printMigrationSummary(MigrationSummary summary) {
        log.info("Migration summary (dryRun={}): report={}, rollback={}",
                properties.isDryRun(), summary.reportCsvPath, summary.rollbackSqlPath);
        for (TargetMigration targetMigration : summary.targetMigrations.values()) {
            log.info("  {}.{} -> scanned={}, pending={}, updated={}, failed={}, skippedAlreadyOss={}, skippedNonHttp={}, retries={}, reusedUpload={}, dryRun={}",
                    targetMigration.target.tableName(),
                    targetMigration.target.columnName(),
                    targetMigration.scanned,
                    targetMigration.pending,
                    targetMigration.updated,
                    targetMigration.failed,
                    targetMigration.skippedAlreadyOss,
                    targetMigration.skippedNonHttp,
                    targetMigration.retryCount,
                    targetMigration.reusedUploadCount,
                    targetMigration.dryRunCount);
        }
    }

    private void printVerificationSummary(VerificationSummary summary) {
        log.info("Verification summary: totalManagedUrlCount={}, sampled={}, success={}, failed={}",
                summary.totalManagedUrlCount, summary.sampled, summary.success, summary.failed);
        if (!summary.failedUrls.isEmpty()) {
            log.warn("Verification failed URLs (showing up to 20):");
            for (int i = 0; i < Math.min(20, summary.failedUrls.size()); i++) {
                log.warn("  {}", summary.failedUrls.get(i));
            }
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(ImageRow row);
    }

    private record TargetColumn(String tableName, String columnName) {
        private String key() {
            return tableName + "." + columnName;
        }
    }

    private record ImageRow(long id, String url) {
    }

    private record DownloadedImage(byte[] bytes, String contentType, long size, URI sourceUri) {
    }

    private record UploadResult(String objectKey, String publicUrl, String contentType, long size) {
    }

    private static class RecoverableMigrationException extends RuntimeException {
        private RecoverableMigrationException(String message) {
            super(message);
        }

        private RecoverableMigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class InventorySummary {
        private final Map<String, TargetInventory> targetInventories = new LinkedHashMap<>();
    }

    private static class TargetInventory {
        private final TargetColumn target;
        private long totalWithValue;
        private long httpUrlCount;
        private long alreadyOssCount;
        private long pendingMigrationCount;

        private TargetInventory(TargetColumn target) {
            this.target = target;
        }
    }

    private static class MigrationSummary {
        private Path reportCsvPath;
        private Path rollbackSqlPath;
        private InventorySummary inventorySummary;
        private final Map<String, TargetMigration> targetMigrations = new LinkedHashMap<>();
    }

    private static class TargetMigration {
        private final TargetColumn target;
        private long scanned;
        private long pending;
        private long updated;
        private long failed;
        private long skippedAlreadyOss;
        private long skippedNonHttp;
        private long retryCount;
        private long reusedUploadCount;
        private long dryRunCount;

        private TargetMigration(TargetColumn target) {
            this.target = target;
        }
    }

    private static class VerificationSummary {
        private long totalManagedUrlCount;
        private long sampled;
        private long success;
        private long failed;
        private final List<String> failedUrls = new ArrayList<>();
    }
}
