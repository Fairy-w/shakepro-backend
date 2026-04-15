package com.shakepro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private int timeoutMs = 45000;

    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
