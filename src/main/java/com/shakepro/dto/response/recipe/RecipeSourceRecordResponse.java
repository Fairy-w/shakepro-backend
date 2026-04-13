package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipeSourceRecordResponse {

    private Long id;
    private String sourceSite;
    private String sourceUrl;
    private String pageType;
    private String rawHtml;
    private String rawText;
    private String status;
    private LocalDateTime scrapedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
