package com.shakepro.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.config.AiConfig;
import com.shakepro.config.AiQwenConfig;
import com.shakepro.dto.request.AiGenerateRecipeByTextRequest;
import com.shakepro.dto.request.AiGenerateRecipeRequest;
import com.shakepro.dto.request.AiRecommendRequest;
import com.shakepro.dto.response.AiGenerateRecipeResponse;
import com.shakepro.dto.response.AiRecommendResponse;
import com.shakepro.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiConfig aiConfig;
    private final AiQwenConfig aiQwenConfig;
    private final HttpClient aiHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiRecommendResponse> recommend(AiRecommendRequest request) {
        if ("mock".equalsIgnoreCase(aiConfig.getProvider())) {
            return getMockRecommendations(request);
        }

        return callExternalAi(request);
    }

    @Override
    public List<AiGenerateRecipeResponse> generateRecipe(AiGenerateRecipeRequest request) {
        return callDashScopeGenerateRecipe(request);
    }

    @Override
    public List<AiGenerateRecipeResponse> generateRecipeByText(AiGenerateRecipeByTextRequest request) {
        return callDashScopeGenerateRecipeByText(request);
    }

    private List<AiRecommendResponse> getMockRecommendations(AiRecommendRequest request) {
        log.info("Using mock AI provider for recommendation, materials={}", request.getMaterials());

        return Arrays.asList(
                AiRecommendResponse.builder()
                        .name("经典莫吉托")
                        .description("清爽薄荷与青柠的完美结合，适合夏日饮用")
                        .materials(Arrays.asList("白朗姆酒 45ml", "青柠汁 20ml", "薄荷叶 8片", "糖浆 15ml", "苏打水 适量"))
                        .steps("1. 将薄荷叶与糖浆轻捣\n2. 加入青柠汁和朗姆酒\n3. 加满冰块\n4. 倒入苏打水\n5. 轻搅后用薄荷叶装饰")
                        .build(),
                AiRecommendResponse.builder()
                        .name("日落特调")
                        .description("根据您选择的材料调配的创意鸡尾酒")
                        .materials(request.getMaterials())
                        .steps("1. 将所有基酒倒入摇酒壶\n2. 加入冰块摇匀\n3. 过滤倒入杯中\n4. 缓慢加入果汁形成分层效果\n5. 装饰后享用")
                        .build()
        );
    }

    private List<AiRecommendResponse> callExternalAi(AiRecommendRequest request) {
        try {
            String prompt = buildPrompt(request);

            Map<String, Object> body = Map.of(
                    "model", aiConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "你是一个专业的鸡尾酒调酒师。根据用户提供的材料推荐鸡尾酒配方。" +
                                    "请返回JSON数组格式，每个对象包含name、description、materials(数组)、steps字段。"),
                            Map.of("role", "user", "content", prompt)
                    )
                    ,
                    "temperature", 0.7
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Calling AI API: provider={}, model={}", aiConfig.getProvider(), aiConfig.getModel());
            // Note: do NOT log apiKey

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .timeout(Duration.ofMillis(aiConfig.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = aiHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("AI API error: status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.AI_ERROR, "AI服务返回异常: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();

            // Parse the AI response JSON
            return objectMapper.readValue(content, new TypeReference<List<AiRecommendResponse>>() {});

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI recommendation failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI推荐服务调用失败: " + e.getMessage());
        }
    }

    private List<AiGenerateRecipeResponse> callDashScopeGenerateRecipe(AiGenerateRecipeRequest request) {
        try {
            String prompt = buildGeneratePrompt(request);

            Map<String, Object> body = Map.of(
                    "model", aiQwenConfig.getModel(),
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of("role", "system", "content",
                                            "你是一个专业的鸡尾酒调酒师。根据用户提供的材料生成鸡尾酒配方。" +
                                                    "请严格返回JSON数组格式，每个对象包含name、description、materials(数组)、steps字段。"),
                                    Map.of("role", "user", "content", prompt)
                            )
                    ),
                    "parameters", Map.of(
                            "temperature", 0.7,
                            "result_format", "message"
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Calling DashScope API for recipe generation, model={}", aiQwenConfig.getModel());

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

            return objectMapper.readValue(content, new TypeReference<List<AiGenerateRecipeResponse>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI recipe generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI配方生成失败: " + e.getMessage());
        }
    }

    private List<AiGenerateRecipeResponse> callDashScopeGenerateRecipeByText(AiGenerateRecipeByTextRequest request) {
        try {
            String prompt = buildGeneratePromptByText(request);

            Map<String, Object> body = Map.of(
                    "model", aiQwenConfig.getModel(),
                    "input", Map.of(
                            "messages", List.of(
                                    Map.of("role", "system", "content",
                                            "你是一个专业的鸡尾酒调酒师。请根据用户自然语言描述生成可执行的鸡尾酒配方。" +
                                                    "请严格返回JSON数组格式，每个对象包含name、description、materials(数组)、steps字段。"),
                                    Map.of("role", "user", "content", prompt)
                            )
                    ),
                    "parameters", Map.of(
                            "temperature", 0.7,
                            "result_format", "message"
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Calling DashScope API for text-to-recipe generation, model={}", aiQwenConfig.getModel());

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

            return objectMapper.readValue(content, new TypeReference<List<AiGenerateRecipeResponse>>() {});
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI text-to-recipe generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_ERROR, "AI自然语言配方生成失败: " + e.getMessage());
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

    private String buildPrompt(AiRecommendRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("我有以下材料：").append(String.join("、", request.getMaterials()));
        if (request.getPreferences() != null && !request.getPreferences().isBlank()) {
            sb.append("\n偏好：").append(request.getPreferences());
        }
        sb.append("\n请推荐2-3款可以制作的鸡尾酒。");
        return sb.toString();
    }

    private String buildGeneratePrompt(AiGenerateRecipeRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("我有以下材料：").append(String.join("、", request.getMaterials()));
        if (request.getPreferences() != null && !request.getPreferences().isBlank()) {
            sb.append("\n偏好：").append(request.getPreferences());
        }
        sb.append("\n请生成2-3款可操作的鸡尾酒配方，并给出详细步骤。");
        return sb.toString();
    }

    private String buildGeneratePromptByText(AiGenerateRecipeByTextRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求如下：").append(request.getPrompt());
        sb.append("\n请根据上述自然语言需求生成2-3款可操作的鸡尾酒配方，并给出详细步骤。");
        return sb.toString();
    }
}
