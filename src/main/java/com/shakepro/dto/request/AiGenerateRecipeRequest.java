package com.shakepro.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AiGenerateRecipeRequest {

    @NotEmpty(message = "材料列表不能为空")
    private List<String> materials;

    private String preferences;
}
