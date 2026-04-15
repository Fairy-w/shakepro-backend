package com.shakepro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.qwen")
public class AiQwenConfig {

    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
    private String apiKey;
    private String model = "qwen-plus";
    private int timeoutMs = 45000;
}
