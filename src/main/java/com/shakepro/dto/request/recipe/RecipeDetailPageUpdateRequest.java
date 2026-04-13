package com.shakepro.dto.request.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecipeDetailPageUpdateRequest {

    @NotBlank(message = "配方ID不能为空")
    @Size(max = 64, message = "配方ID最长64位")
    private String id;

    @NotBlank(message = "中文名不能为空")
    @Size(max = 50, message = "中文名最长50位")
    private String name;

    @Size(max = 100, message = "英文名最长100位")
    private String englishName;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类最长50位")
    private String category;

    @NotBlank(message = "主图不能为空")
    @Size(max = 255, message = "主图地址最长255位")
    private String heroImage;

    @Size(max = 100, message = "强调文案最长100位")
    private String highlight;

    @NotBlank(message = "副标题不能为空")
    @Size(max = 150, message = "副标题最长150位")
    private String subtitle;

    @NotBlank(message = "简介不能为空")
    @Size(max = 255, message = "简介最长255位")
    private String description;

    @Size(max = 255, message = "场景化描述最长255位")
    private String story;

    @NotBlank(message = "适合场景不能为空")
    @Size(max = 100, message = "适合场景最长100位")
    private String bestFor;

    @NotBlank(message = "制作难度不能为空")
    @Size(max = 20, message = "制作难度最长20位")
    private String difficulty;

    @NotBlank(message = "制作时长不能为空")
    @Size(max = 20, message = "制作时长最长20位")
    private String duration;

    @NotBlank(message = "酒精度不能为空")
    @Size(max = 20, message = "酒精度最长20位")
    private String abv;

    @NotBlank(message = "出杯容量不能为空")
    @Size(max = 20, message = "出杯容量最长20位")
    private String volume;

    @NotBlank(message = "杯型不能为空")
    @Size(max = 100, message = "杯型最长100位")
    private String glass;

    @NotBlank(message = "装饰不能为空")
    @Size(max = 100, message = "装饰最长100位")
    private String garnish;

    @NotBlank(message = "饮用温度不能为空")
    @Size(max = 50, message = "饮用温度最长50位")
    private String serveTemperature;

    @NotEmpty(message = "风味标签不能为空")
    private List<@NotBlank(message = "风味标签项不能为空") @Size(max = 10, message = "风味标签项最长10位") String> flavorTags = new ArrayList<>();

    @Valid
    @NotEmpty(message = "风味维度不能为空")
    private List<FlavorMetricItemRequest> flavorMetrics = new ArrayList<>();

    private List<@NotBlank(message = "搭配建议项不能为空") @Size(max = 50, message = "搭配建议项最长50位") String> pairings = new ArrayList<>();

    private List<@NotBlank(message = "饮用提示项不能为空") @Size(max = 100, message = "饮用提示项最长100位") String> serviceNotes = new ArrayList<>();

    @Valid
    @NotEmpty(message = "配料不能为空")
    private List<IngredientItemRequest> ingredients = new ArrayList<>();

    @Valid
    @NotEmpty(message = "步骤不能为空")
    private List<StepItemRequest> steps = new ArrayList<>();

    @Data
    public static class FlavorMetricItemRequest {

        @NotBlank(message = "风味维度名不能为空")
        @Size(max = 10, message = "风味维度名最长10位")
        private String label;

        @NotNull(message = "风味维度值不能为空")
        @Min(value = 1, message = "风味维度值不能小于1")
        @Max(value = 5, message = "风味维度值不能大于5")
        private Integer value;
    }

    @Data
    public static class IngredientItemRequest {

        @NotBlank(message = "材料名不能为空")
        @Size(max = 50, message = "材料名最长50位")
        private String name;

        @NotBlank(message = "用量不能为空")
        @Size(max = 20, message = "用量最长20位")
        private String amount;

        @Size(max = 100, message = "材料说明最长100位")
        private String note;
    }

    @Data
    public static class StepItemRequest {

        @NotBlank(message = "步骤标题不能为空")
        @Size(max = 50, message = "步骤标题最长50位")
        private String title;

        @NotBlank(message = "步骤说明不能为空")
        @Size(max = 255, message = "步骤说明最长255位")
        private String detail;

        @Size(max = 100, message = "步骤提示最长100位")
        private String hint;
    }
}
