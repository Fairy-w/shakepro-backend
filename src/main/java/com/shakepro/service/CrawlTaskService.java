package com.shakepro.service;

import com.shakepro.dto.request.recipe.RecipeCrawlTaskRequest;
import com.shakepro.dto.response.recipe.RecipeCrawlTaskResponse;
import com.shakepro.dto.response.recipe.RecipeSourceRecordResponse;

import java.util.List;

public interface CrawlTaskService {

    RecipeCrawlTaskResponse crawl(RecipeCrawlTaskRequest request);

    RecipeSourceRecordResponse getSourceRecord(Long sourceRecordId);

    List<RecipeSourceRecordResponse> listSourceRecordsByStatus(String status);

    RecipeSourceRecordResponse rejectSourceRecord(Long sourceRecordId);
}
