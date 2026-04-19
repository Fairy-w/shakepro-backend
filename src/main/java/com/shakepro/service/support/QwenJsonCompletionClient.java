package com.shakepro.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.config.AiQwenConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QwenJsonCompletionClient {

    private final AiQwenConfig aiQwenConfig;
    @Qualifier("aiHttpClient")
    private final HttpClient aiHttpClient;
    private final ObjectMapper objectMapper;

    public String completeJson(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", aiQwenConfig.getModel(),
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userPrompt)
                            )
                    ),
                    "parameters", Map.of(
                            "temperature", 0.3,
                            "result_format", "message"
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiQwenConfig.getBaseUrl() + "/services/aigc/text-generation/generation"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiQwenConfig.getApiKey())
                    .timeout(Duration.ofMillis(aiQwenConfig.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = aiHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("DashScope API error: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.AI_ERROR, "AI服务返回异常: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractDashScopeContent(root);
            if (content == null || content.isBlank()) {
                throw new BusinessException(ErrorCode.AI_ERROR, "AI返回内容为空");
            }

            return stripMarkdownCodeFence(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DashScope JSON completion failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI调用失败: " + e.getMessage());
        }
    }

    private String extractDashScopeContent(JsonNode root) {
        String fromMessage = root.path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(null);
        if (fromMessage != null && !fromMessage.isBlank()) {
            return fromMessage;
        }
        return root.path("output").path("text").asText(null);
    }

    private String stripMarkdownCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineBreak = trimmed.indexOf('\n');
        if (firstLineBreak < 0) {
            return trimmed;
        }

        String withoutHeader = trimmed.substring(firstLineBreak + 1);
        int lastFence = withoutHeader.lastIndexOf("```");
        if (lastFence >= 0) {
            return withoutHeader.substring(0, lastFence).trim();
        }
        return withoutHeader.trim();
    }
}
