package com.shakepro.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.request.admin.AdminGeneratedCocktailSaveRequest;
import com.shakepro.dto.request.admin.AdminPageBatchImportRequest;
import com.shakepro.dto.request.admin.AdminPageFieldExtractRequest;
import com.shakepro.dto.response.admin.AdminBatchImportHistoryResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStartResponse;
import com.shakepro.dto.response.admin.AdminBatchImportJobStatusResponse;
import com.shakepro.dto.response.admin.AdminPageResult;
import com.shakepro.dto.response.admin.AdminPageBatchImportResponse;
import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;
import com.shakepro.dto.response.admin.AdminPageTextResponse;
import com.shakepro.entity.Cocktail;
import com.shakepro.entity.CrawlBatchImportHistory;
import com.shakepro.entity.CrawlImportRecord;
import com.shakepro.repository.CocktailRepository;
import com.shakepro.repository.CrawlBatchImportHistoryRepository;
import com.shakepro.repository.CrawlImportRecordRepository;
import com.shakepro.service.AdminPageAiGenerateService;
import com.shakepro.service.AdminPageBatchImportService;
import com.shakepro.service.AdminPageCrawlService;
import com.shakepro.service.AdminPageExtractService;
import com.shakepro.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPageBatchImportServiceImpl implements AdminPageBatchImportService {

    private static final Pattern LINK_HREF_PATTERN = Pattern.compile("<a[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_LD_SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern NEXT_DATA_SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern ASSET_PATH_PATTERN = Pattern.compile(".*\\.(?:css|js|json|xml|txt|png|jpe?g|gif|webp|svg|ico|pdf|zip|gz|mp4|mp3|woff2?)$");
    private static final Pattern SITEMAP_LOC_PATTERN = Pattern.compile("<loc>(.*?)</loc>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LOCALE_PREFIX_PATTERN = Pattern.compile("^/[a-z]{2}(?:-[a-z]{2})?(?=/|$)");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MIN_SCORE_TO_KEEP = 20;
    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String IMPORT_STATUS_IGNORED_NON_DETAIL = "IGNORED_NON_DETAIL";
    private static final String ITEM_STATUS_SKIPPED = "SKIPPED";
    private static final String BATCH_STATUS_SUCCESS = "SUCCESS";
    private static final String BATCH_STATUS_PARTIAL = "PARTIAL";
    private static final String BATCH_STATUS_FAILED = "FAILED";
    private static final String BATCH_STATUS_SKIPPED = "SKIPPED";
    private static final String JOB_STATUS_PENDING = "PENDING";
    private static final String JOB_STATUS_RUNNING = "RUNNING";
    private static final Set<String> BLOCKED_PATH_PREFIXES = Set.of(
            "/var/",
            "/_next/",
            "/api/",
            "/img/",
            "/assets/",
            "/fonts/",
            "/static/",
            "/video-sitemap",
            "/sitemap"
    );
    private static final Set<String> NON_DETAIL_PATH_PREFIXES = Set.of(
            "/service/",
            "/journal/",
            "/products/",
            "/agency",
            "/events",
            "/scanner",
            "/courses",
            "/hospitality",
            "/bar-set",
            "/cart",
            "/checkout",
            "/login",
            "/register",
            "/my-account"
    );

    private final AdminPageCrawlService adminPageCrawlService;
    private final AdminPageExtractService adminPageExtractService;
    private final AdminPageAiGenerateService adminPageAiGenerateService;
    private final AdminService adminService;
    private final CocktailRepository cocktailRepository;
    private final CrawlBatchImportHistoryRepository crawlBatchImportHistoryRepository;
    private final CrawlImportRecordRepository crawlImportRecordRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, BatchImportJobState> jobStates = new ConcurrentHashMap<>();
    private final ExecutorService asyncJobExecutor = Executors.newFixedThreadPool(2);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public AdminPageBatchImportResponse importFromList(AdminPageBatchImportRequest request) {
        return importFromListInternal(request, null);
    }

    @Override
    public AdminBatchImportJobStartResponse startImportJob(AdminPageBatchImportRequest request) {
        String jobId = UUID.randomUUID().toString().replace("-", "");
        AdminPageBatchImportRequest jobRequest = copyRequest(request);
        BatchImportJobState state = new BatchImportJobState(jobId, jobRequest);
        jobStates.put(jobId, state);

        asyncJobExecutor.submit(() -> {
            state.markRunning();
            try {
                AdminPageBatchImportResponse response = importFromListInternal(jobRequest, state);
                state.markCompleted(response);
            } catch (Exception ex) {
                state.markFailed(resolveErrorMessage(ex));
                log.warn("批量抓取异步任务失败: jobId={}, message={}", jobId, resolveErrorMessage(ex));
            }
        });

        return AdminBatchImportJobStartResponse.builder()
                .jobId(jobId)
                .status(JOB_STATUS_PENDING)
                .message("批量抓取任务已提交")
                .build();
    }

    @Override
    public AdminBatchImportJobStatusResponse getImportJobStatus(String jobId) {
        BatchImportJobState state = jobStates.get(jobId);
        if (state == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或已过期");
        }
        return state.toResponse();
    }

    @Override
    public AdminPageResult<AdminBatchImportHistoryResponse> listImportHistories(int page, int size) {
        int pageNo = Math.max(0, page);
        int pageSize = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminBatchImportHistoryResponse> pageData =
                crawlBatchImportHistoryRepository.findAll(pageable).map(AdminBatchImportHistoryResponse::from);
        return AdminPageResult.from(pageData);
    }

    private AdminPageBatchImportResponse importFromListInternal(AdminPageBatchImportRequest request, BatchImportJobState progressState) {
        long start = System.currentTimeMillis();
        int maxItems = normalizeMaxItems(request.getMaxItems());
        int requestedConcurrency = normalizeConcurrency(request.getConcurrency());
        boolean autoGenerate = Boolean.TRUE.equals(request.getAutoGenerate());
        boolean autoSave = Boolean.TRUE.equals(request.getAutoSave());
        boolean onlyNew = request.getOnlyNew() == null || Boolean.TRUE.equals(request.getOnlyNew());
        String historyListUrl = trimToNull(request.getListUrl());
        String historyListTitle = null;
        int historyDiscoveredCount = 0;
        int historySelectedCount = 0;
        int historyProcessedCount = 0;
        int historySuccessCount = 0;
        int historyFailureCount = 0;
        int historyRemainingUnimportedCount = 0;
        String historyStatus = BATCH_STATUS_FAILED;
        String historyErrorMessage = null;

        try {
            AdminPageTextResponse listPage = adminPageCrawlService.crawlPageText(request.getListUrl());
            historyListUrl = listPage.getUrl();
            historyListTitle = trimToNull(listPage.getTitle());

            List<String> discoveredUrls = extractDetailUrls(listPage.getUrl(), listPage.getHtml());
            historyDiscoveredCount = discoveredUrls.size();

            List<String> candidateUrls = discoveredUrls.stream().limit(maxItems).toList();
            if (candidateUrls.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "列表页未识别到可抓取的详情链接，请检查网址或站点结构");
            }

            List<String> selectedUrls = onlyNew ? filterOnlyNewCandidates(candidateUrls) : candidateUrls;
            historySelectedCount = selectedUrls.size();
            if (progressState != null) {
                progressState.setPreparedInfo(listPage.getUrl(), listPage.getTitle(), discoveredUrls.size(), selectedUrls.size());
            }
            if (selectedUrls.isEmpty()) {
                historyStatus = BATCH_STATUS_SKIPPED;
                int remainingUnimportedCount = calculateRemainingUnimportedCount(discoveredUrls);
                historyRemainingUnimportedCount = remainingUnimportedCount;
                if (progressState != null) {
                    progressState.setRemainingUnimportedCount(remainingUnimportedCount);
                }
                return AdminPageBatchImportResponse.builder()
                        .listUrl(listPage.getUrl())
                        .listTitle(listPage.getTitle())
                        .discoveredCount(discoveredUrls.size())
                        .selectedCount(0)
                        .processedCount(0)
                        .successCount(0)
                        .failureCount(0)
                        .remainingUnimportedCount(remainingUnimportedCount)
                        .durationMs(System.currentTimeMillis() - start)
                        .items(new ArrayList<>())
                        .build();
            }

            int concurrency = normalizeConcurrency(requestedConcurrency, selectedUrls.size());
            ExecutorService executor = Executors.newFixedThreadPool(concurrency);
            List<AdminPageBatchImportResponse.ImportItemResult> items;
            try {
                AtomicInteger processedCounter = new AtomicInteger(0);
                AtomicInteger successCounter = new AtomicInteger(0);
                AtomicInteger failureCounter = new AtomicInteger(0);
                List<CompletableFuture<AdminPageBatchImportResponse.ImportItemResult>> futures = new ArrayList<>();
                for (int i = 0; i < selectedUrls.size(); i++) {
                    final int index = i + 1;
                    final String detailUrl = selectedUrls.get(i);
                    futures.add(CompletableFuture.supplyAsync(
                            () -> processSingleUrl(index, detailUrl, listPage.getUrl(), autoGenerate, autoSave),
                            executor
                    ).whenComplete((item, throwable) -> {
                        int processed = processedCounter.incrementAndGet();
                        if (throwable == null && item != null && isSuccessItemStatus(item.getStatus())) {
                            successCounter.incrementAndGet();
                        } else if (throwable != null || item == null || isFailureItemStatus(item.getStatus())) {
                            failureCounter.incrementAndGet();
                        }
                        if (progressState != null) {
                            progressState.setProcessedInfo(
                                    processed,
                                    successCounter.get(),
                                    failureCounter.get(),
                                    item == null ? null : item.getUrl(),
                                    item == null ? null : item.getStage()
                            );
                        }
                    }));
                }

                items = futures.stream()
                        .map(CompletableFuture::join)
                        .sorted(Comparator.comparingInt(AdminPageBatchImportResponse.ImportItemResult::getIndex))
                        .toList();
            } finally {
                executor.shutdown();
            }

            int successCount = (int) items.stream().filter(item -> isSuccessItemStatus(item.getStatus())).count();
            int failureCount = (int) items.stream().filter(item -> isFailureItemStatus(item.getStatus())).count();
            int remainingUnimportedCount = calculateRemainingUnimportedCount(discoveredUrls);
            historyProcessedCount = items.size();
            historySuccessCount = successCount;
            historyFailureCount = failureCount;
            historyRemainingUnimportedCount = remainingUnimportedCount;
            historyStatus = failureCount == 0 ? BATCH_STATUS_SUCCESS : (successCount > 0 ? BATCH_STATUS_PARTIAL : BATCH_STATUS_FAILED);
            if (progressState != null) {
                progressState.setRemainingUnimportedCount(remainingUnimportedCount);
            }

            return AdminPageBatchImportResponse.builder()
                    .listUrl(listPage.getUrl())
                    .listTitle(listPage.getTitle())
                    .discoveredCount(discoveredUrls.size())
                    .selectedCount(selectedUrls.size())
                    .processedCount(items.size())
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .remainingUnimportedCount(remainingUnimportedCount)
                    .durationMs(System.currentTimeMillis() - start)
                    .items(items)
                    .build();
        } catch (Exception ex) {
            historyErrorMessage = resolveErrorMessage(ex);
            historyStatus = BATCH_STATUS_FAILED;
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            saveBatchImportHistory(
                    historyListUrl,
                    historyListTitle,
                    onlyNew,
                    maxItems,
                    requestedConcurrency,
                    autoGenerate,
                    autoSave,
                    historyDiscoveredCount,
                    historySelectedCount,
                    historyProcessedCount,
                    historySuccessCount,
                    historyFailureCount,
                    historyRemainingUnimportedCount,
                    durationMs,
                    historyStatus,
                    historyErrorMessage
            );
        }
    }

    private AdminPageBatchImportResponse.ImportItemResult processSingleUrl(
            int index,
            String detailUrl,
            String listUrl,
            boolean autoGenerate,
            boolean autoSave) {
        String stage = "crawl";
        String title = null;
        AdminPageExtractFieldsResponse currentFields = null;
        try {
            AdminPageTextResponse page = adminPageCrawlService.crawlPageText(detailUrl);
            title = page.getTitle();

            stage = "extract";
            AdminPageFieldExtractRequest extractRequest = new AdminPageFieldExtractRequest();
            extractRequest.setUrl(page.getUrl());
            extractRequest.setTitle(page.getTitle());
            extractRequest.setHtml(page.getHtml());
            currentFields = adminPageExtractService.extractFields(extractRequest);

            if (shouldSkipAsNonDetailPage(detailUrl)) {
                String message = "识别为列表或合集页，已跳过 AI 生成与入库";
                upsertImportRecord(detailUrl, listUrl, IMPORT_STATUS_IGNORED_NON_DETAIL, null, message);
                return buildSkippedItemResult(index, detailUrl, "precheck", title, currentFields, message, null);
            }

            if (autoSave) {
                Cocktail existing = cocktailRepository.findBySourceUrl(detailUrl).orElse(null);
                if (existing != null) {
                    String message = "该来源链接已入库，已跳过";
                    upsertImportRecord(detailUrl, listUrl, IMPORT_STATUS_SUCCESS, existing.getId(), message);
                    return buildSkippedItemResult(index, detailUrl, "precheck", title, currentFields, message, existing.getId());
                }
            }

            if (autoGenerate || autoSave) {
                stage = "generate";
                currentFields = adminPageAiGenerateService.generateChineseFields(currentFields);
            }

            Long savedCocktailId = null;
            if (autoSave) {
                stage = "save";
                Cocktail existingAfterGenerate = cocktailRepository.findBySourceUrl(detailUrl).orElse(null);
                if (existingAfterGenerate != null) {
                    String message = "该来源链接已入库，已跳过";
                    upsertImportRecord(detailUrl, listUrl, IMPORT_STATUS_SUCCESS, existingAfterGenerate.getId(), message);
                    return buildSkippedItemResult(index, detailUrl, "precheck", title, currentFields, message, existingAfterGenerate.getId());
                }
                validateForSave(currentFields);
                savedCocktailId = adminService.createGeneratedCocktail(buildSaveRequest(currentFields, detailUrl)).getId();
            }
            String recordStatus = autoSave ? IMPORT_STATUS_SUCCESS : "PROCESSED";
            upsertImportRecord(detailUrl, listUrl, recordStatus, savedCocktailId, null);

            return AdminPageBatchImportResponse.ImportItemResult.builder()
                    .index(index)
                    .url(detailUrl)
                    .status(IMPORT_STATUS_SUCCESS)
                    .stage("done")
                    .title(title)
                    .name(currentFields == null ? null : currentFields.getName())
                    .savedCocktailId(savedCocktailId)
                    .missingFields(currentFields == null ? new ArrayList<>() : currentFields.getMissingFields())
                    .fields(currentFields)
                    .build();
        } catch (Exception ex) {
            if ("save".equals(stage)) {
                Cocktail existing = cocktailRepository.findBySourceUrl(detailUrl).orElse(null);
                if (existing != null) {
                    String message = "该来源链接已入库，已跳过";
                    upsertImportRecord(detailUrl, listUrl, IMPORT_STATUS_SUCCESS, existing.getId(), message);
                    return buildSkippedItemResult(index, detailUrl, "precheck", title, currentFields, message, existing.getId());
                }
            }
            String message = resolveErrorMessage(ex);
            log.warn("批量导入处理失败: url={}, stage={}, message={}", detailUrl, stage, message);
            upsertImportRecord(detailUrl, listUrl, IMPORT_STATUS_FAILED, null, message);
            return AdminPageBatchImportResponse.ImportItemResult.builder()
                    .index(index)
                    .url(detailUrl)
                    .status(IMPORT_STATUS_FAILED)
                    .stage(stage)
                    .title(title)
                    .name(currentFields == null ? null : currentFields.getName())
                    .errorMessage(message)
                    .missingFields(currentFields == null ? new ArrayList<>() : currentFields.getMissingFields())
                    .fields(currentFields)
                    .build();
        }
    }

    private AdminPageBatchImportResponse.ImportItemResult buildSkippedItemResult(
            int index,
            String detailUrl,
            String stage,
            String title,
            AdminPageExtractFieldsResponse fields,
            String message,
            Long savedCocktailId) {
        return AdminPageBatchImportResponse.ImportItemResult.builder()
                .index(index)
                .url(detailUrl)
                .status(ITEM_STATUS_SKIPPED)
                .stage(stage)
                .title(title)
                .name(fields == null ? null : fields.getName())
                .savedCocktailId(savedCocktailId)
                .errorMessage(message)
                .missingFields(fields == null ? new ArrayList<>() : fields.getMissingFields())
                .fields(fields)
                .build();
    }

    private List<String> filterOnlyNewCandidates(List<String> candidateUrls) {
        if (candidateUrls == null || candidateUrls.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> handledUrls = crawlImportRecordRepository.findAlreadyHandledDetailUrlsIn(candidateUrls);
        if (handledUrls.isEmpty()) {
            return candidateUrls;
        }
        Set<String> imported = new LinkedHashSet<>(handledUrls);
        return candidateUrls.stream().filter(url -> !imported.contains(url)).toList();
    }

    private boolean shouldSkipAsNonDetailPage(String detailUrl) {
        String path = extractPath(detailUrl);
        if (!isKnownListingPath(path)) {
            return false;
        }
        // 对已知列表/合集路径做强制短路，避免进入 AI 与入库。
        return true;
    }

    private String extractPath(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawUrl);
            return uri.getPath() == null ? "" : uri.getPath();
        } catch (Exception ex) {
            return "";
        }
    }

    private void upsertImportRecord(
            String detailUrl,
            String listUrl,
            String status,
            Long savedCocktailId,
            String errorMessage) {
        CrawlImportRecord record = crawlImportRecordRepository.findByDetailUrl(detailUrl)
                .orElseGet(() -> CrawlImportRecord.builder()
                        .detailUrl(detailUrl)
                        .build());
        record.setListUrl(trimToNull(listUrl));
        record.setStatus(status);
        record.setSavedCocktailId(savedCocktailId);
        record.setErrorMessage(trimToNull(errorMessage));
        record.setLastCrawledAt(LocalDateTime.now());
        crawlImportRecordRepository.save(record);
    }

    private void saveBatchImportHistory(
            String listUrl,
            String listTitle,
            boolean onlyNew,
            int maxItems,
            int concurrency,
            boolean autoGenerate,
            boolean autoSave,
            int discoveredCount,
            int selectedCount,
            int processedCount,
            int successCount,
            int failureCount,
            int remainingUnimportedCount,
            long durationMs,
            String status,
            String errorMessage) {
        try {
            String normalizedListUrl = trimToNull(listUrl);
            String normalizedStatus = trimToNull(status);
            CrawlBatchImportHistory history = CrawlBatchImportHistory.builder()
                    .listUrl(normalizedListUrl == null ? "" : normalizedListUrl)
                    .listTitle(trimToNull(listTitle))
                    .onlyNew(onlyNew)
                    .maxItems(maxItems)
                    .concurrency(concurrency)
                    .autoGenerate(autoGenerate)
                    .autoSave(autoSave)
                    .discoveredCount(Math.max(0, discoveredCount))
                    .selectedCount(Math.max(0, selectedCount))
                    .processedCount(Math.max(0, processedCount))
                    .successCount(Math.max(0, successCount))
                    .failureCount(Math.max(0, failureCount))
                    .remainingUnimportedCount(Math.max(0, remainingUnimportedCount))
                    .durationMs(Math.max(0, durationMs))
                    .status(normalizedStatus == null ? BATCH_STATUS_FAILED : normalizedStatus)
                    .errorMessage(trimToNull(errorMessage))
                    .build();
            crawlBatchImportHistoryRepository.save(history);
        } catch (Exception ex) {
            log.warn("保存批量抓取历史失败: listUrl={}, message={}", listUrl, ex.getMessage());
        }
    }

    private int calculateRemainingUnimportedCount(List<String> discoveredUrls) {
        if (discoveredUrls == null || discoveredUrls.isEmpty()) {
            return 0;
        }
        Set<String> uniqueDiscovered = new LinkedHashSet<>(discoveredUrls);
        Set<String> imported = new LinkedHashSet<>(crawlImportRecordRepository.findSuccessfulDetailUrlsIn(uniqueDiscovered));
        imported.addAll(cocktailRepository.findExistingSourceUrlsIn(uniqueDiscovered));
        return Math.max(0, uniqueDiscovered.size() - imported.size());
    }

    private boolean isSuccessItemStatus(String status) {
        return IMPORT_STATUS_SUCCESS.equals(status) || ITEM_STATUS_SKIPPED.equals(status);
    }

    private boolean isFailureItemStatus(String status) {
        return IMPORT_STATUS_FAILED.equals(status);
    }

    private AdminPageBatchImportRequest copyRequest(AdminPageBatchImportRequest request) {
        AdminPageBatchImportRequest copied = new AdminPageBatchImportRequest();
        copied.setListUrl(trimToEmpty(request.getListUrl()));
        copied.setMaxItems(request.getMaxItems());
        copied.setConcurrency(request.getConcurrency());
        copied.setAutoGenerate(request.getAutoGenerate());
        copied.setAutoSave(request.getAutoSave());
        copied.setOnlyNew(request.getOnlyNew());
        return copied;
    }

    private int normalizeMaxItems(Integer requested) {
        if (requested == null) {
            return 50;
        }
        return Math.max(1, Math.min(1000, requested));
    }

    private int normalizeConcurrency(Integer requested) {
        if (requested == null) {
            return 3;
        }
        return Math.max(1, Math.min(8, requested));
    }

    private int normalizeConcurrency(Integer requested, int size) {
        return Math.max(1, Math.min(size, requested));
    }

    private List<String> extractDetailUrls(String listUrl, String html) {
        URI listUri = URI.create(listUrl);
        String listHost = listUri.getHost() == null ? "" : listUri.getHost().toLowerCase(Locale.ROOT);
        String normalizedListUrl = normalizeUrl(listUri);
        Set<String> collected = new LinkedHashSet<>();

        collectAnchorUrls(html, listUri, listHost, normalizedListUrl, collected);
        collectJsonLdUrls(html, listUri, listHost, normalizedListUrl, collected);
        collectNextDataUrls(html, listUri, listHost, normalizedListUrl, collected);
        List<String> ranked = rankCandidateUrls(listUri, collected);
        if (!ranked.isEmpty()) {
            return ranked;
        }

        Set<String> sitemapCandidates = collectFromSitemap(listUri, listHost, normalizedListUrl);
        return rankCandidateUrls(listUri, sitemapCandidates);
    }

    private void collectAnchorUrls(
            String html,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> collected) {
        if (html == null || html.isBlank()) {
            return;
        }
        Matcher matcher = LINK_HREF_PATTERN.matcher(html);
        while (matcher.find()) {
            addCandidateUrl(matcher.group(1), listUri, listHost, normalizedListUrl, collected);
        }
    }

    private void collectJsonLdUrls(
            String html,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> collected) {
        if (html == null || html.isBlank()) {
            return;
        }
        Matcher matcher = JSON_LD_SCRIPT_PATTERN.matcher(html);
        while (matcher.find()) {
            String jsonText = matcher.group(1);
            if (jsonText == null || jsonText.isBlank()) {
                continue;
            }
            try {
                JsonNode root = objectMapper.readTree(jsonText);
                collectPossibleUrls(root, listUri, listHost, normalizedListUrl, collected);
            } catch (Exception ignored) {
                // 忽略异常 JSON-LD，继续处理其余链接。
            }
        }
    }

    private void collectNextDataUrls(
            String html,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> collected) {
        if (html == null || html.isBlank()) {
            return;
        }
        Matcher matcher = NEXT_DATA_SCRIPT_PATTERN.matcher(html);
        if (!matcher.find()) {
            return;
        }

        String jsonText = matcher.group(1);
        if (jsonText == null || jsonText.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            collectPossibleUrls(root, listUri, listHost, normalizedListUrl, collected);
        } catch (Exception ignored) {
            // 忽略异常 NEXT_DATA，避免阻断整个批量流程。
        }
    }

    private void collectPossibleUrls(
            JsonNode node,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> collected) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                collectPossibleUrls(item, listUri, listHost, normalizedListUrl, collected);
            }
            return;
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                JsonNode value = entry.getValue();
                if (value != null && looksLikeUrlFieldKey(key)) {
                    if (value.isTextual()) {
                        addCandidateUrl(value.asText(), listUri, listHost, normalizedListUrl, collected);
                        return;
                    }
                    if (value.isArray()) {
                        for (JsonNode item : value) {
                            if (item != null && item.isTextual()) {
                                addCandidateUrl(item.asText(), listUri, listHost, normalizedListUrl, collected);
                            } else {
                                collectPossibleUrls(item, listUri, listHost, normalizedListUrl, collected);
                            }
                        }
                        return;
                    }
                }
                if (value != null && value.isContainerNode()) {
                    collectPossibleUrls(value, listUri, listHost, normalizedListUrl, collected);
                }
            });
        }
    }

    private boolean looksLikeUrlFieldKey(String key) {
        return "url".equals(key)
                || "href".equals(key)
                || "canonical".equals(key)
                || "canonicalurl".equals(key)
                || "@id".equals(key)
                || "permalink".equals(key);
    }

    private List<String> rankCandidateUrls(URI listUri, Set<String> candidates) {
        return candidates.stream()
                .map(url -> {
                    try {
                        URI uri = URI.create(url);
                        return new RankedCandidate(url, scoreCandidate(listUri, uri));
                    } catch (Exception ex) {
                        return new RankedCandidate(url, Integer.MIN_VALUE);
                    }
                })
                .filter(candidate -> candidate.score() >= MIN_SCORE_TO_KEEP)
                .sorted(Comparator.comparingInt(RankedCandidate::score).reversed())
                .map(RankedCandidate::url)
                .toList();
    }

    private Set<String> collectFromSitemap(URI listUri, String listHost, String normalizedListUrl) {
        Set<String> candidates = new LinkedHashSet<>();
        Set<String> visitedSitemaps = new LinkedHashSet<>();

        URI sitemapUri = listUri.resolve("/sitemap.xml");
        collectSitemapUrls(sitemapUri, listUri, listHost, normalizedListUrl, candidates, visitedSitemaps, 2);
        if (!visitedSitemaps.contains(normalizeUrl(listUri.resolve("/sitemap_index.xml")))) {
            collectSitemapUrls(listUri.resolve("/sitemap_index.xml"), listUri, listHost, normalizedListUrl, candidates, visitedSitemaps, 2);
        }
        return candidates;
    }

    private void collectSitemapUrls(
            URI sitemapUri,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> candidates,
            Set<String> visitedSitemaps,
            int depth) {
        if (depth < 0 || sitemapUri == null) {
            return;
        }
        String normalizedSitemapUri = normalizeUrl(sitemapUri);
        if (!visitedSitemaps.add(normalizedSitemapUri)) {
            return;
        }

        String xml = fetchUrlBody(sitemapUri);
        if (xml == null || xml.isBlank()) {
            return;
        }

        Matcher matcher = SITEMAP_LOC_PATTERN.matcher(xml);
        while (matcher.find()) {
            String rawLoc = matcher.group(1);
            if (rawLoc == null || rawLoc.isBlank()) {
                continue;
            }
            URI locUri;
            try {
                locUri = URI.create(rawLoc.trim());
            } catch (Exception ex) {
                continue;
            }

            String path = locUri.getPath() == null ? "" : locUri.getPath().toLowerCase(Locale.ROOT);
            if (path.endsWith(".xml")) {
                collectSitemapUrls(locUri, listUri, listHost, normalizedListUrl, candidates, visitedSitemaps, depth - 1);
                continue;
            }
            addCandidateUrl(locUri.toString(), listUri, listHost, normalizedListUrl, candidates);
        }
    }

    private String fetchUrlBody(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept", "application/xml,text/xml,text/html;q=0.9,*/*;q=0.8")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    private int scoreCandidate(URI listUri, URI candidateUri) {
        if (candidateUri == null) {
            return Integer.MIN_VALUE;
        }
        String path = candidateUri.getPath() == null ? "" : candidateUri.getPath().toLowerCase(Locale.ROOT);
        if (path.isBlank() || "/".equals(path)) {
            return Integer.MIN_VALUE;
        }
        if (isKnownListingPath(path)) {
            return Integer.MIN_VALUE;
        }

        int score = 0;
        boolean listIsCocktail = isCocktailListPath(listUri.getPath());

        if (path.contains("/cocktails/")) {
            score += 120;
            if (!path.contains("/collections/")) {
                score += 40;
            }
        }
        if (path.contains("/recipes/")) {
            score += 90;
        }
        if (path.contains("/collections/")) {
            score -= 120;
        }
        if (looksLikeTerminalDetailPath(path)) {
            score += 35;
        }
        if (isNonDetailPath(path)) {
            score -= 50;
        }
        if (listIsCocktail) {
            score += (path.contains("/cocktails/") || path.contains("/recipes/")) ? 25 : -30;
        }
        return score;
    }

    private boolean isCocktailListPath(String listPath) {
        if (listPath == null) {
            return false;
        }
        String lower = listPath.toLowerCase(Locale.ROOT);
        return lower.contains("cocktail") || lower.contains("recipe");
    }

    private boolean isNonDetailPath(String path) {
        for (String prefix : NON_DETAIL_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnownListingPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        String withoutLocale = stripLocalePrefix(normalized);
        return "/cocktails".equals(withoutLocale)
                || "/recipes".equals(withoutLocale)
                || "/cocktails/collections".equals(withoutLocale)
                || "/recipes/collections".equals(withoutLocale)
                || withoutLocale.startsWith("/cocktails/collections/")
                || withoutLocale.startsWith("/recipes/collections/");
    }

    private String stripLocalePrefix(String normalizedPath) {
        Matcher matcher = LOCALE_PREFIX_PATTERN.matcher(normalizedPath);
        if (!matcher.find()) {
            return normalizedPath;
        }
        String remainder = normalizedPath.substring(matcher.end());
        return remainder.isEmpty() ? "/" : remainder;
    }

    private boolean looksLikeTerminalDetailPath(String path) {
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == trimmed.length() - 1) {
            return false;
        }
        String lastSegment = trimmed.substring(lastSlash + 1);
        return lastSegment.length() >= 3 && !lastSegment.contains(".");
    }

    private void addCandidateUrl(
            String rawValue,
            URI listUri,
            String listHost,
            String normalizedListUrl,
            Set<String> collected) {
        if (rawValue == null) {
            return;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()
                || trimmed.startsWith("#")
                || trimmed.startsWith("javascript:")
                || trimmed.startsWith("mailto:")
                || trimmed.startsWith("tel:")) {
            return;
        }

        URI resolved;
        try {
            resolved = listUri.resolve(trimmed);
        } catch (Exception ex) {
            return;
        }

        String scheme = resolved.getScheme() == null ? "" : resolved.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return;
        }

        String host = resolved.getHost() == null ? "" : resolved.getHost().toLowerCase(Locale.ROOT);
        if (!isSameSiteHost(host, listHost)) {
            return;
        }

        if (!isLikelyHtmlDetailPage(resolved)) {
            return;
        }

        String normalized = normalizeUrl(resolved);
        if (normalized.equals(normalizedListUrl)) {
            return;
        }
        collected.add(normalized);
    }

    private boolean isLikelyHtmlDetailPage(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath().trim();
        if (path.isEmpty() || "/".equals(path)) {
            return false;
        }
        String lowerPath = path.toLowerCase(Locale.ROOT);
        for (String blockedPrefix : BLOCKED_PATH_PREFIXES) {
            if (lowerPath.startsWith(blockedPrefix)) {
                return false;
            }
        }
        if (isKnownListingPath(lowerPath)) {
            return false;
        }
        if (lowerPath.contains("/locales")) {
            return false;
        }
        return !ASSET_PATH_PATTERN.matcher(lowerPath).matches();
    }

    private boolean isSameSiteHost(String candidateHost, String listHost) {
        if (candidateHost == null || candidateHost.isBlank() || listHost == null || listHost.isBlank()) {
            return false;
        }
        if (candidateHost.equals(listHost)) {
            return true;
        }
        return ("www." + candidateHost).equals(listHost) || ("www." + listHost).equals(candidateHost);
    }

    private String normalizeUrl(URI uri) {
        try {
            URI cleaned = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
            return cleaned.toString();
        } catch (URISyntaxException ex) {
            return uri.toString();
        }
    }

    private void validateForSave(AdminPageExtractFieldsResponse fields) {
        if (fields == null || fields.getName() == null || fields.getName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保存失败：缺少鸡尾酒名称");
        }
        List<AdminPageExtractFieldsResponse.StepItem> steps = sanitizeSteps(fields.getSteps());
        if (steps.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保存失败：步骤为空");
        }
        List<AdminPageExtractFieldsResponse.IngredientItem> ingredients = sanitizeIngredients(fields.getIngredients());
        if (ingredients.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保存失败：配方材料为空");
        }
    }

    private AdminGeneratedCocktailSaveRequest buildSaveRequest(AdminPageExtractFieldsResponse fields, String sourceUrl) {
        AdminGeneratedCocktailSaveRequest request = new AdminGeneratedCocktailSaveRequest();
        request.setName(trimToEmpty(fields.getName()));
        request.setEnglishName(trimToNull(fields.getEnglishName()));
        request.setCategory(trimToNull(fields.getCategory()));
        request.setHeroImage(trimToNull(fields.getHeroImage()));
        request.setDifficulty(trimToNull(fields.getDifficulty()));
        request.setAbv(trimToNull(fields.getAbv()));
        request.setGlass(trimToNull(fields.getGlass()));
        request.setGarnish(trimToNull(fields.getGarnish()));
        request.setHighlight(trimToNull(fields.getHighlight()));
        request.setSubtitle(trimToNull(fields.getSubtitle()));
        request.setDescription(trimToNull(fields.getDescription()));
        request.setStory(trimToNull(fields.getStory()));
        request.setSourceUrl(trimToNull(sourceUrl));
        request.setFlavorTags(sanitizeStringList(fields.getFlavorTags()));
        request.setPairings(sanitizeStringList(fields.getPairings()));
        request.setServiceNotes(sanitizeStringList(fields.getServiceNotes()));

        List<AdminGeneratedCocktailSaveRequest.FlavorMetricItemRequest> flavorMetrics = new ArrayList<>();
        if (fields.getFlavorMetrics() != null) {
            fields.getFlavorMetrics().forEach((name, value) -> {
                String metricName = trimToNull(name);
                if (metricName == null || value == null) {
                    return;
                }
                AdminGeneratedCocktailSaveRequest.FlavorMetricItemRequest item = new AdminGeneratedCocktailSaveRequest.FlavorMetricItemRequest();
                item.setName(metricName);
                item.setValue(Math.max(0, Math.min(5, value)));
                flavorMetrics.add(item);
            });
        }
        request.setFlavorMetrics(flavorMetrics);

        List<AdminGeneratedCocktailSaveRequest.IngredientItemRequest> ingredients = new ArrayList<>();
        for (AdminPageExtractFieldsResponse.IngredientItem item : sanitizeIngredients(fields.getIngredients())) {
            AdminGeneratedCocktailSaveRequest.IngredientItemRequest ingredient = new AdminGeneratedCocktailSaveRequest.IngredientItemRequest();
            ingredient.setName(trimToEmpty(item.getName()));
            ingredient.setAmount(trimToNull(item.getAmount()));
            ingredient.setNote(trimToNull(item.getNote()));
            ingredients.add(ingredient);
        }
        request.setIngredients(ingredients);

        List<AdminGeneratedCocktailSaveRequest.StepItemRequest> steps = new ArrayList<>();
        for (AdminPageExtractFieldsResponse.StepItem item : sanitizeSteps(fields.getSteps())) {
            AdminGeneratedCocktailSaveRequest.StepItemRequest step = new AdminGeneratedCocktailSaveRequest.StepItemRequest();
            step.setTitle(trimToNull(item.getTitle()));
            step.setDetail(trimToEmpty(item.getDetail()));
            steps.add(step);
        }
        request.setSteps(steps);
        return request;
    }

    private List<String> sanitizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                normalized.add(cleaned);
            }
        }
        return normalized;
    }

    private List<AdminPageExtractFieldsResponse.IngredientItem> sanitizeIngredients(
            List<AdminPageExtractFieldsResponse.IngredientItem> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return new ArrayList<>();
        }
        List<AdminPageExtractFieldsResponse.IngredientItem> normalized = new ArrayList<>();
        for (AdminPageExtractFieldsResponse.IngredientItem item : ingredients) {
            if (item == null) {
                continue;
            }
            String name = trimToNull(item.getName());
            if (name == null) {
                continue;
            }
            normalized.add(AdminPageExtractFieldsResponse.IngredientItem.builder()
                    .name(name)
                    .amount(trimToNull(item.getAmount()))
                    .note(trimToNull(item.getNote()))
                    .build());
        }
        return normalized;
    }

    private List<AdminPageExtractFieldsResponse.StepItem> sanitizeSteps(
            List<AdminPageExtractFieldsResponse.StepItem> steps) {
        if (steps == null || steps.isEmpty()) {
            return new ArrayList<>();
        }
        List<AdminPageExtractFieldsResponse.StepItem> normalized = new ArrayList<>();
        for (AdminPageExtractFieldsResponse.StepItem item : steps) {
            if (item == null) {
                continue;
            }
            String detail = trimToNull(item.getDetail());
            if (detail == null) {
                continue;
            }
            normalized.add(AdminPageExtractFieldsResponse.StepItem.builder()
                    .title(trimToNull(item.getTitle()))
                    .detail(detail)
                    .build());
        }
        return normalized;
    }

    private String resolveErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        if (current instanceof BusinessException businessException) {
            return businessException.getMessage();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return "处理失败，请稍后重试";
        }
        return message;
    }

    private String toFinalBatchStatus(AdminPageBatchImportResponse response) {
        if (response == null) {
            return BATCH_STATUS_FAILED;
        }
        if (response.getSelectedCount() == 0) {
            return BATCH_STATUS_SKIPPED;
        }
        if (response.getFailureCount() == 0) {
            return BATCH_STATUS_SUCCESS;
        }
        return response.getSuccessCount() > 0 ? BATCH_STATUS_PARTIAL : BATCH_STATUS_FAILED;
    }

    private class BatchImportJobState {
        private final String jobId;
        private final long startedAtEpochMs;
        private volatile long updatedAtEpochMs;
        private volatile String status;
        private volatile String message;
        private volatile String listUrl;
        private volatile String listTitle;
        private volatile Integer maxItems;
        private volatile Integer concurrency;
        private volatile Boolean autoGenerate;
        private volatile Boolean autoSave;
        private volatile Boolean onlyNew;
        private volatile Integer discoveredCount;
        private volatile Integer selectedCount;
        private volatile Integer processedCount;
        private volatile Integer successCount;
        private volatile Integer failureCount;
        private volatile Integer remainingUnimportedCount;
        private volatile Integer progressPercent;
        private volatile Long durationMs;
        private volatile String currentUrl;
        private volatile String currentStage;
        private volatile String errorMessage;

        private BatchImportJobState(String jobId, AdminPageBatchImportRequest request) {
            this.jobId = jobId;
            this.startedAtEpochMs = System.currentTimeMillis();
            this.updatedAtEpochMs = this.startedAtEpochMs;
            this.status = JOB_STATUS_PENDING;
            this.message = "任务等待执行";
            this.listUrl = trimToNull(request.getListUrl());
            this.maxItems = normalizeMaxItems(request.getMaxItems());
            this.concurrency = normalizeConcurrency(request.getConcurrency());
            this.autoGenerate = Boolean.TRUE.equals(request.getAutoGenerate());
            this.autoSave = Boolean.TRUE.equals(request.getAutoSave());
            this.onlyNew = request.getOnlyNew() == null || Boolean.TRUE.equals(request.getOnlyNew());
            this.discoveredCount = 0;
            this.selectedCount = 0;
            this.processedCount = 0;
            this.successCount = 0;
            this.failureCount = 0;
            this.remainingUnimportedCount = 0;
            this.progressPercent = 0;
            this.durationMs = 0L;
        }

        private synchronized void markRunning() {
            this.status = JOB_STATUS_RUNNING;
            this.message = "任务执行中";
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized void setRemainingUnimportedCount(int remainingUnimportedCount) {
            this.remainingUnimportedCount = Math.max(0, remainingUnimportedCount);
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized void setPreparedInfo(String listUrl, String listTitle, int discoveredCount, int selectedCount) {
            this.listUrl = trimToNull(listUrl);
            this.listTitle = trimToNull(listTitle);
            this.discoveredCount = Math.max(0, discoveredCount);
            this.selectedCount = Math.max(0, selectedCount);
            this.progressPercent = selectedCount <= 0 ? 100 : Math.min(99, (int) Math.floor((this.processedCount * 100.0) / selectedCount));
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized void setProcessedInfo(int processedCount, int successCount, int failureCount, String currentUrl, String currentStage) {
            this.processedCount = Math.max(0, processedCount);
            this.successCount = Math.max(0, successCount);
            this.failureCount = Math.max(0, failureCount);
            this.currentUrl = trimToNull(currentUrl);
            this.currentStage = trimToNull(currentStage);
            if (this.selectedCount != null && this.selectedCount > 0) {
                this.progressPercent = Math.min(99, (int) Math.floor((this.processedCount * 100.0) / this.selectedCount));
            } else {
                this.progressPercent = 0;
            }
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized void markCompleted(AdminPageBatchImportResponse response) {
            this.listUrl = trimToNull(response.getListUrl());
            this.listTitle = trimToNull(response.getListTitle());
            this.discoveredCount = response.getDiscoveredCount();
            this.selectedCount = response.getSelectedCount();
            this.processedCount = response.getProcessedCount();
            this.successCount = response.getSuccessCount();
            this.failureCount = response.getFailureCount();
            this.remainingUnimportedCount = response.getRemainingUnimportedCount();
            this.durationMs = response.getDurationMs();
            this.status = toFinalBatchStatus(response);
            this.progressPercent = 100;
            this.currentStage = "done";
            this.currentUrl = null;
            this.errorMessage = null;
            this.message = "任务已完成";
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized void markFailed(String message) {
            this.status = BATCH_STATUS_FAILED;
            this.errorMessage = trimToNull(message);
            this.message = this.errorMessage == null ? "任务执行失败" : this.errorMessage;
            this.durationMs = System.currentTimeMillis() - this.startedAtEpochMs;
            this.progressPercent = Math.min(99, this.progressPercent == null ? 0 : this.progressPercent);
            this.updatedAtEpochMs = System.currentTimeMillis();
        }

        private synchronized AdminBatchImportJobStatusResponse toResponse() {
            long duration = this.durationMs == null || this.durationMs <= 0
                    ? Math.max(0L, System.currentTimeMillis() - this.startedAtEpochMs)
                    : this.durationMs;
            return AdminBatchImportJobStatusResponse.builder()
                    .jobId(this.jobId)
                    .status(this.status)
                    .message(this.message)
                    .listUrl(this.listUrl)
                    .listTitle(this.listTitle)
                    .maxItems(this.maxItems)
                    .concurrency(this.concurrency)
                    .autoGenerate(this.autoGenerate)
                    .autoSave(this.autoSave)
                    .onlyNew(this.onlyNew)
                    .discoveredCount(this.discoveredCount)
                    .selectedCount(this.selectedCount)
                    .processedCount(this.processedCount)
                    .successCount(this.successCount)
                    .failureCount(this.failureCount)
                    .remainingUnimportedCount(this.remainingUnimportedCount)
                    .progressPercent(this.progressPercent)
                    .durationMs(duration)
                    .currentUrl(this.currentUrl)
                    .currentStage(this.currentStage)
                    .startedAtEpochMs(this.startedAtEpochMs)
                    .updatedAtEpochMs(this.updatedAtEpochMs)
                    .errorMessage(this.errorMessage)
                    .build();
        }
    }

    private record RankedCandidate(String url, int score) {
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
