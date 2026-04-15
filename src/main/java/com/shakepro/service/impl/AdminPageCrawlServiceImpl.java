package com.shakepro.service.impl;

import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.dto.response.admin.AdminPageTextResponse;
import com.shakepro.service.AdminPageCrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminPageCrawlServiceImpl implements AdminPageCrawlService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("[\\t\\x0B\\f\\r ]+");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public AdminPageTextResponse crawlPageText(String url) {
        URI uri = parseAndValidateUrl(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build();

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVER_ERROR, "网页抓取被中断，请稍后重试");
        } catch (IOException e) {
            log.warn("抓取网页失败: url={}, message={}", uri, e.getMessage());
            throw new BusinessException(ErrorCode.SERVER_ERROR, "网页抓取失败，请检查网址是否可访问");
        }

        if (response.statusCode() >= 400) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, buildHttpErrorMessage(response.statusCode()));
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.isBlank() && !contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该网址返回的不是网页内容");
        }

        Charset charset = resolveCharset(contentType);
        String html = new String(response.body(), charset);
        if (html.isBlank()) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "网页源码为空，暂时无法继续处理");
        }

        return AdminPageTextResponse.builder()
                .url(uri.toString())
                .title(extractTitle(html))
                .html(html)
                .build();
    }

    private URI parseAndValidateUrl(String rawUrl) {
        String trimmedUrl = rawUrl == null ? "" : rawUrl.trim();
        if (trimmedUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "网址不能为空");
        }

        URI uri;
        try {
            uri = new URI(trimmedUrl);
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "网址格式不正确");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 http 或 https 网页地址");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "网址缺少有效域名");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTS.contains(normalizedHost) || isPrivateIpv4(normalizedHost)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持抓取本地或内网地址");
        }

        return uri;
    }

    private Charset resolveCharset(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        String[] segments = contentType.split(";");
        for (String segment : segments) {
            String trimmedSegment = segment.trim();
            if (!trimmedSegment.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                continue;
            }

            String charsetName = trimmedSegment.substring("charset=".length()).trim().replace("\"", "");
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                log.warn("网页声明了无法识别的字符集: {}", charsetName);
                return StandardCharsets.UTF_8;
            }
        }

        return StandardCharsets.UTF_8;
    }

    private String extractTitle(String html) {
        var matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return normalizeInlineText(HtmlUtils.htmlUnescape(matcher.group(1)));
    }

    private String buildHttpErrorMessage(int statusCode) {
        if (statusCode == 403) {
            return "目标页面返回 403，说明站点可能存在反爬限制或需要浏览器环境访问";
        }
        if (statusCode == 401) {
            return "目标页面需要授权后才能访问，当前抓取器无法直接读取";
        }
        if (statusCode == 429) {
            return "目标页面触发了访问频率限制，请稍后再试";
        }
        return "目标页面暂时无法访问，状态码: " + statusCode;
    }

    private String normalizeInlineText(String value) {
        return MULTI_SPACE_PATTERN.matcher(value).replaceAll(" ").trim();
    }

    private boolean isPrivateIpv4(String host) {
        String[] segments = host.split("\\.");
        if (segments.length != 4) {
            return false;
        }

        int[] numbers = new int[4];
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            if (!segment.matches("\\d{1,3}")) {
                return false;
            }

            int value = Integer.parseInt(segment);
            if (value < 0 || value > 255) {
                return false;
            }
            numbers[index] = value;
        }

        int first = numbers[0];
        int second = numbers[1];
        return first == 10
                || first == 0
                || first == 127
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }
}
