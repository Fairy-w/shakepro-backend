package com.shakepro.dto.request.admin;

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
public class AdminGeneratedCocktailSaveRequest {

    @NotBlank(message = "鸡尾酒名称不能为空")
    @Size(max = 100, message = "鸡尾酒名称最长100位")
    private String name;

    @Size(max = 255, message = "英文名最长255位")
    private String englishName;

    @Size(max = 100, message = "分类最长100位")
    private String category;

    @Size(max = 500, message = "主图地址最长500位")
    private String heroImage;

    @Size(max = 50, message = "难度最长50位")
    private String difficulty;

    @Size(max = 32, message = "abv 最长32位")
    private String abv;

    @Size(max = 100, message = "杯型最长100位")
    private String glass;

    @Size(max = 255, message = "装饰最长255位")
    private String garnish;

    @Size(max = 5000, message = "亮点文案长度不能超过5000")
    private String highlight;

    @Size(max = 5000, message = "副标题长度不能超过5000")
    private String subtitle;

    @Size(max = 5000, message = "描述长度不能超过5000")
    private String description;

    @Size(max = 5000, message = "故事长度不能超过5000")
    private String story;

    @Size(max = 768, message = "来源链接最长768位")
    private String sourceUrl;

    @Valid
    @NotNull(message = "风味标签不能为空")
    private List<@NotBlank(message = "风味标签不能为空") @Size(max = 100, message = "风味标签最长100位") String> flavorTags = new ArrayList<>();

    @Valid
    @NotNull(message = "风味指标不能为空")
    private List<FlavorMetricItemRequest> flavorMetrics = new ArrayList<>();

    @Valid
    @NotNull(message = "搭配建议不能为空")
    private List<@NotBlank(message = "搭配建议不能为空") @Size(max = 255, message = "搭配建议最长255位") String> pairings = new ArrayList<>();

    @Valid
    @NotNull(message = "服务备注不能为空")
    private List<@NotBlank(message = "服务备注不能为空") @Size(max = 500, message = "服务备注最长500位") String> serviceNotes = new ArrayList<>();

    @Valid
    @NotEmpty(message = "材料列表不能为空")
    private List<IngredientItemRequest> ingredients = new ArrayList<>();

    @Valid
    @NotEmpty(message = "步骤列表不能为空")
    private List<StepItemRequest> steps = new ArrayList<>();

    @Data
    public static class FlavorMetricItemRequest {
        @NotBlank(message = "风味指标名不能为空")
        @Size(max = 100, message = "风味指标名最长100位")
        private String name;

        @NotNull(message = "风味指标值不能为空")
        @Min(value = 0, message = "风味指标值不能小于0")
        @Max(value = 5, message = "风味指标值不能大于5")
        private Integer value;
    }

    @Data
    public static class IngredientItemRequest {
        private Long materialId;

        @NotBlank(message = "材料名称不能为空")
        @Size(max = 255, message = "材料名称最长255位")
        private String name;

        @Size(max = 50, message = "用量最长50位")
        private String amount;

        @Size(max = 500, message = "材料备注最长500位")
        private String note;
    }

    @Data
    public static class StepItemRequest {
        @Size(max = 100, message = "步骤标题最长100位")
        private String title;

        @NotBlank(message = "步骤详情不能为空")
        @Size(max = 5000, message = "步骤详情长度不能超过5000")
        private String detail;
    }
}
