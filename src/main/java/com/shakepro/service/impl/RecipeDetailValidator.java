package com.shakepro.service.impl;

import com.shakepro.dto.response.recipe.RecipeDetailPageResponse;
import com.shakepro.dto.response.recipe.RecipeDetailValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecipeDetailValidator {

    public RecipeDetailValidationResult validate(RecipeDetailPageResponse detail) {
        List<String> errors = new ArrayList<>();

        requireText(detail.getId(), "id", 1, 64, errors);
        requireText(detail.getName(), "name", 2, 8, errors);
        optionalText(detail.getEnglishName(), "englishName", 1, 24, errors);
        requireText(detail.getCategory(), "category", 1, 24, errors);
        requireText(detail.getHeroImage(), "heroImage", 1, 255, errors);
        optionalText(detail.getHighlight(), "highlight", 1, 22, errors);
        requireText(detail.getSubtitle(), "subtitle", 1, 28, errors);
        requireText(detail.getDescription(), "description", 20, 60, errors);
        optionalText(detail.getStory(), "story", 20, 70, errors);
        requireText(detail.getBestFor(), "bestFor", 6, 20, errors);
        requireText(detail.getDifficulty(), "difficulty", 2, 6, errors);
        requireText(detail.getDuration(), "duration", 2, 10, errors);
        requireText(detail.getAbv(), "abv", 2, 8, errors);
        requireText(detail.getVolume(), "volume", 2, 10, errors);
        requireText(detail.getGlass(), "glass", 2, 16, errors);
        requireText(detail.getGarnish(), "garnish", 2, 16, errors);
        requireText(detail.getServeTemperature(), "serveTemperature", 2, 12, errors);

        validateStringList(detail.getFlavorTags(), "flavorTags", 2, 4, 2, 4, true, errors);
        validateFlavorMetrics(detail.getFlavorMetrics(), errors);
        validateStringList(detail.getPairings(), "pairings", 0, 3, 4, 12, false, errors);
        validateStringList(detail.getServiceNotes(), "serviceNotes", 0, 3, 12, 28, false, errors);
        validateIngredients(detail.getIngredients(), errors);
        validateSteps(detail.getSteps(), errors);

        return RecipeDetailValidationResult.builder()
                .passed(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private void validateFlavorMetrics(List<RecipeDetailPageResponse.FlavorMetricItemResponse> metrics, List<String> errors) {
        if (metrics == null || metrics.size() < 4 || metrics.size() > 6) {
            errors.add("flavorMetrics 项数必须在 4 到 6 之间");
            return;
        }
        for (int i = 0; i < metrics.size(); i++) {
            RecipeDetailPageResponse.FlavorMetricItemResponse item = metrics.get(i);
            if (item == null) {
                errors.add("flavorMetrics[" + i + "] 不能为空");
                continue;
            }
            requireText(item.getLabel(), "flavorMetrics[" + i + "].label", 2, 4, errors);
            if (item.getValue() == null || item.getValue() < 1 || item.getValue() > 5) {
                errors.add("flavorMetrics[" + i + "].value 必须在 1 到 5 之间");
            }
        }
    }

    private void validateIngredients(List<RecipeDetailPageResponse.IngredientItemResponse> ingredients, List<String> errors) {
        if (ingredients == null || ingredients.size() < 3 || ingredients.size() > 8) {
            errors.add("ingredients 项数必须在 3 到 8 之间");
            return;
        }
        for (int i = 0; i < ingredients.size(); i++) {
            RecipeDetailPageResponse.IngredientItemResponse item = ingredients.get(i);
            if (item == null) {
                errors.add("ingredients[" + i + "] 不能为空");
                continue;
            }
            requireText(item.getName(), "ingredients[" + i + "].name", 2, 12, errors);
            requireText(item.getAmount(), "ingredients[" + i + "].amount", 1, 12, errors);
            optionalText(item.getNote(), "ingredients[" + i + "].note", 8, 24, errors);
        }
    }

    private void validateSteps(List<RecipeDetailPageResponse.StepItemResponse> steps, List<String> errors) {
        if (steps == null || steps.size() < 3 || steps.size() > 6) {
            errors.add("steps 项数必须在 3 到 6 之间");
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            RecipeDetailPageResponse.StepItemResponse item = steps.get(i);
            if (item == null) {
                errors.add("steps[" + i + "] 不能为空");
                continue;
            }
            requireText(item.getTitle(), "steps[" + i + "].title", 2, 8, errors);
            requireText(item.getDetail(), "steps[" + i + "].detail", 16, 40, errors);
            optionalText(item.getHint(), "steps[" + i + "].hint", 8, 20, errors);
        }
    }

    private void validateStringList(
            List<String> values,
            String field,
            int minItems,
            int maxItems,
            int minLength,
            int maxLength,
            boolean required,
            List<String> errors
    ) {
        if (values == null || values.isEmpty()) {
            if (required) {
                errors.add(field + " 不能为空");
            }
            return;
        }
        if (values.size() < minItems || values.size() > maxItems) {
            errors.add(field + " 项数必须在 " + minItems + " 到 " + maxItems + " 之间");
        }
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            requireText(value, field + "[" + i + "]", minLength, maxLength, errors);
        }
    }

    private void requireText(String value, String field, int minLength, int maxLength, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " 不能为空");
            return;
        }
        int length = value.trim().length();
        if (length < minLength || length > maxLength) {
            errors.add(field + " 长度必须在 " + minLength + " 到 " + maxLength + " 之间");
        }
    }

    private void optionalText(String value, String field, int minLength, int maxLength, List<String> errors) {
        if (value == null || value.isBlank()) {
            return;
        }
        int length = value.trim().length();
        if (length < minLength || length > maxLength) {
            errors.add(field + " 长度必须在 " + minLength + " 到 " + maxLength + " 之间");
        }
    }
}
