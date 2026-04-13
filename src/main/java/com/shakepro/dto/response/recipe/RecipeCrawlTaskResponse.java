package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeCrawlTaskResponse {

    private String sourceSite;
    private String entryUrl;
    private String crawlMode;
    private Integer maxPages;
    private Integer maxItems;
    private Boolean fetchDetailPages;
    private Integer totalSaved;
    private List<RecipeSourceRecordResponse> records;
}
