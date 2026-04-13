package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.common.util.RecipePipelineStatuses;
import com.shakepro.dto.request.recipe.RecipeCrawlTaskRequest;
import com.shakepro.dto.response.recipe.RecipeCrawlTaskResponse;
import com.shakepro.dto.response.recipe.RecipeSourceRecordResponse;
import com.shakepro.entity.RecipeSourceRecord;
import com.shakepro.repository.RecipeSourceRecordRepository;
import com.shakepro.service.CrawlTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlTaskServiceImpl implements CrawlTaskService {

    private static final Pattern HREF_PATTERN = Pattern.compile("(?is)href=[\"']([^\"'#]+)[\"']");

    private final HttpClient aiHttpClient;
    private final RecipeSourceRecordRepository recipeSourceRecordRepository;

    @Override
    @Transactional
    public RecipeCrawlTaskResponse crawl(RecipeCrawlTaskRequest request) {
        PageSnapshot entryPage = fetchPage(request.getEntryUrl());
        List<RecipeSourceRecord> records = new ArrayList<>();
        records.add(saveOrUpdateSourceRecord(request.getSourceSite(), request.getEntryUrl(), "list", entryPage));

        if (Boolean.TRUE.equals(request.getFetchDetailPages())) {
            Set<String> detailUrls = extractDetailUrls(request.getEntryUrl(), entryPage.html(), request.getMaxItems());
            for (String detailUrl : detailUrls) {
                PageSnapshot detailPage = fetchPage(detailUrl);
                records.add(saveOrUpdateSourceRecord(request.getSourceSite(), detailUrl, "detail", detailPage));
            }
        }

        return RecipeCrawlTaskResponse.builder()
                .sourceSite(request.getSourceSite())
                .entryUrl(request.getEntryUrl())
                .crawlMode(request.getCrawlMode())
                .maxPages(request.getMaxPages())
                .maxItems(request.getMaxItems())
                .fetchDetailPages(request.getFetchDetailPages())
                .totalSaved(records.size())
                .records(records.stream().map(this::toResponse).toList())
                .build();
    }

    @Override
    public RecipeSourceRecordResponse getSourceRecord(Long sourceRecordId) {
        RecipeSourceRecord record = recipeSourceRecordRepository.findById(sourceRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "原始采集记录不存在"));
        return toResponse(record);
    }

    @Override
    public List<RecipeSourceRecordResponse> listSourceRecordsByStatus(String status) {
        List<RecipeSourceRecord> records;
        if (status == null || status.isBlank()) {
            records = recipeSourceRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "scrapedAt"));
        } else {
            records = recipeSourceRecordRepository.findByStatusOrderByScrapedAtDesc(status);
        }
        return records.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecipeSourceRecordResponse rejectSourceRecord(Long sourceRecordId) {
        RecipeSourceRecord record = recipeSourceRecordRepository.findById(sourceRecordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "原始采集记录不存在"));
        record.setStatus(RecipePipelineStatuses.REJECTED);
        return toResponse(recipeSourceRecordRepository.save(record));
    }

    private PageSnapshot fetchPage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "抓取页面失败: " + url);
            }
            String html = response.body();
            return new PageSnapshot(html, stripHtml(html));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Crawl page failed: url={}, message={}", url, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "抓取页面失败: " + url);
        }
    }

    private RecipeSourceRecord saveOrUpdateSourceRecord(String sourceSite, String sourceUrl, String pageType, PageSnapshot pageSnapshot) {
        Optional<RecipeSourceRecord> existing = recipeSourceRecordRepository.findBySourceUrl(sourceUrl);
        RecipeSourceRecord record = existing.orElseGet(RecipeSourceRecord::new);
        record.setSourceSite(sourceSite);
        record.setSourceUrl(sourceUrl);
        record.setPageType(pageType);
        record.setRawHtml(pageSnapshot.html());
        record.setRawText(pageSnapshot.text());
        record.setStatus(RecipePipelineStatuses.SCRAPED);
        return recipeSourceRecordRepository.save(record);
    }

    private Set<String> extractDetailUrls(String entryUrl, String html, Integer maxItems) {
        Set<String> urls = new LinkedHashSet<>();
        Matcher matcher = HREF_PATTERN.matcher(html);
        URI entryUri = URI.create(entryUrl);
        while (matcher.find()) {
            String href = matcher.group(1).trim();
            URI resolved = entryUri.resolve(href);
            String resolvedUrl = resolved.toString();
            if (!resolved.getHost().equalsIgnoreCase(entryUri.getHost())) {
                continue;
            }
            if (resolvedUrl.contains("/iba-cocktail/") || resolvedUrl.contains("/cocktail/")) {
                urls.add(resolvedUrl);
            }
            if (maxItems != null && urls.size() >= maxItems) {
                break;
            }
        }
        return urls;
    }

    private String stripHtml(String html) {
        return cleanText(html
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)</div>", "\n")
                .replaceAll("(?is)<[^>]+>", " "));
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private RecipeSourceRecordResponse toResponse(RecipeSourceRecord record) {
        return RecipeSourceRecordResponse.builder()
                .id(record.getId())
                .sourceSite(record.getSourceSite())
                .sourceUrl(record.getSourceUrl())
                .pageType(record.getPageType())
                .rawHtml(record.getRawHtml())
                .rawText(record.getRawText())
                .status(record.getStatus())
                .scrapedAt(record.getScrapedAt())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private record PageSnapshot(String html, String text) {
    }
}
