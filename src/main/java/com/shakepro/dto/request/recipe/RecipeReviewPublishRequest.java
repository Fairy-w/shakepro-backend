package com.shakepro.dto.request.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecipeReviewPublishRequest {

    @NotNull(message = "详情内容ID不能为空")
    private Long detailContentId;

    @NotBlank(message = "审核动作不能为空")
    @Size(max = 20, message = "审核动作最长20位")
    private String action;

    @Size(max = 500, message = "审核备注最长500位")
    private String reviewComment;

    private Boolean publishNow;

    @Valid
    private RecipeDetailPageUpdateRequest detail;
}
