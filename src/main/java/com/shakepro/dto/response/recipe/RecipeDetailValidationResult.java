package com.shakepro.dto.response.recipe;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecipeDetailValidationResult {

    private boolean passed;
    private List<String> errors;
}
