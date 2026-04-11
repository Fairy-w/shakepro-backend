package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiCocktailFavoriteCreateRequest {

    @Size(max = 128, message = "recipeKey长度不能超过128")
    private String recipeKey;

    @NotBlank(message = "酒名不能为空")
    @Size(max = 100, message = "酒名长度不能超过100")
    private String name;

    @Size(max = 5000, message = "风味描述长度不能超过5000")
    private String description;

    @NotEmpty(message = "材料列表不能为空")
    private List<@NotBlank(message = "材料项不能为空") @Size(max = 200, message = "材料项长度不能超过200") String> materials;

    @NotEmpty(message = "步骤列表不能为空")
    private List<@NotBlank(message = "步骤项不能为空") @Size(max = 1000, message = "步骤项长度不能超过1000") String> steps;

    @Size(max = 5000, message = "调酒需求长度不能超过5000")
    private String prompt;

    @Size(max = 32, message = "source长度不能超过32")
    private String source;
}
