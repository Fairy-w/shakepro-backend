package com.shakepro.dto.request.admin;

import com.shakepro.dto.response.admin.AdminPageExtractFieldsResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdminPageAiGenerateRequest {

    @NotBlank(message = "来源网址不能为空")
    @Size(max = 1000, message = "来源网址长度不能超过1000个字符")
    private String url;

    @Size(max = 300, message = "页面标题长度不能超过300个字符")
    private String title;

    @Size(max = 100, message = "提取模式长度不能超过100个字符")
    private String extractMode;

    @Size(max = 300, message = "名称长度不能超过300个字符")
    private String name;

    @Size(max = 300, message = "英文名长度不能超过300个字符")
    private String englishName;

    @Size(max = 200, message = "分类长度不能超过200个字符")
    private String category;

    @Size(max = 2000, message = "主图地址长度不能超过2000个字符")
    private String heroImage;

    @Size(max = 200, message = "难度长度不能超过200个字符")
    private String difficulty;

    @Size(max = 100, message = "abv 长度不能超过100个字符")
    private String abv;

    @Size(max = 200, message = "杯型长度不能超过200个字符")
    private String glass;

    @Size(max = 300, message = "装饰长度不能超过300个字符")
    private String garnish;

    @Size(max = 500, message = "highlight 长度不能超过500个字符")
    private String highlight;

    @Size(max = 500, message = "subtitle 长度不能超过500个字符")
    private String subtitle;

    @Size(max = 5000, message = "description 长度不能超过5000个字符")
    private String description;

    @Size(max = 5000, message = "story 长度不能超过5000个字符")
    private String story;

    @NotNull(message = "flavorTags 不能为空")
    private List<String> flavorTags = new ArrayList<>();

    @NotNull(message = "flavorMetrics 不能为空")
    private Map<String, Integer> flavorMetrics = new LinkedHashMap<>();

    @NotNull(message = "pairings 不能为空")
    private List<String> pairings = new ArrayList<>();

    @NotNull(message = "serviceNotes 不能为空")
    private List<String> serviceNotes = new ArrayList<>();

    @NotNull(message = "ingredients 不能为空")
    private List<AdminPageExtractFieldsResponse.IngredientItem> ingredients = new ArrayList<>();

    @NotNull(message = "steps 不能为空")
    private List<AdminPageExtractFieldsResponse.StepItem> steps = new ArrayList<>();

    @NotNull(message = "fieldSources 不能为空")
    private Map<String, AdminPageExtractFieldsResponse.FieldSource> fieldSources = new LinkedHashMap<>();

    @NotNull(message = "missingFields 不能为空")
    private List<String> missingFields = new ArrayList<>();

    public AdminPageExtractFieldsResponse toExtractedResponse() {
        return AdminPageExtractFieldsResponse.builder()
                .url(url)
                .title(title)
                .extractMode(extractMode)
                .name(name)
                .englishName(englishName)
                .category(category)
                .heroImage(heroImage)
                .difficulty(difficulty)
                .abv(abv)
                .glass(glass)
                .garnish(garnish)
                .highlight(highlight)
                .subtitle(subtitle)
                .description(description)
                .story(story)
                .flavorTags(new ArrayList<>(flavorTags))
                .flavorMetrics(new LinkedHashMap<>(flavorMetrics))
                .pairings(new ArrayList<>(pairings))
                .serviceNotes(new ArrayList<>(serviceNotes))
                .ingredients(new ArrayList<>(ingredients))
                .steps(new ArrayList<>(steps))
                .fieldSources(new LinkedHashMap<>(fieldSources))
                .missingFields(new ArrayList<>(missingFields))
                .build();
    }
}
