package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGenerateRecipeByTextRequest {

    @NotBlank(message = "自然语言描述不能为空")
    private String prompt;
}
