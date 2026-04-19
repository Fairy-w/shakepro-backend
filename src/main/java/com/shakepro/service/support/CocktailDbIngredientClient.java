package com.shakepro.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shakepro.common.exception.BusinessException;
import com.shakepro.common.result.ErrorCode;
import com.shakepro.config.CocktailDbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CocktailDbIngredientClient {

    private final CocktailDbProperties cocktailDbProperties;
    private final ObjectMapper objectMapper;

    public List<String> listIngredients() {
        if (!cocktailDbProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "cocktaildb.enabled=false，已禁用同步");
        }

        String url = cocktailDbProperties.getNormalizedBaseUrl()
                + "/api/json/v1/"
                + cocktailDbProperties.getNormalizedApiKey()
                + "/list.php?i=list";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cocktailDbProperties.getConnectTimeoutMs()))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(cocktailDbProperties.getReadTimeoutMs()))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SERVER_ERROR, "拉取TheCocktailDB材料失败，HTTP " + response.statusCode());
            }
            return parseIngredientNames(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVER_ERROR, "拉取TheCocktailDB材料失败: " + e.getMessage());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "拉取TheCocktailDB材料失败: " + e.getMessage());
        }
    }

    private List<String> parseIngredientNames(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode drinks = root.path("drinks");
        if (!drinks.isArray()) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "TheCocktailDB返回格式异常：缺少drinks数组");
        }

        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : drinks) {
            String name = item.path("strIngredient1").asText(null);
            if (name != null && !name.isBlank()) {
                values.add(name.trim());
            }
        }
        log.info("Fetched ingredients from TheCocktailDB, size={}", values.size());
        return List.copyOf(values);
    }
}
