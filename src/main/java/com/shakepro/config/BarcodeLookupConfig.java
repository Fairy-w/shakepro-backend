package com.shakepro.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "barcode.lookup")
public class BarcodeLookupConfig {

    private boolean enabled = true;
    private String baseUrl = "https://world.openfoodfacts.org";
    private int timeoutMs = 3500;
    private int cacheHours = 168;
    private String userAgent = "ShakeProBackend/1.0";

    @Bean
    public HttpClient barcodeHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
