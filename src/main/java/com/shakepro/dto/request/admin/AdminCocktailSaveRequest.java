package com.shakepro.dto.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminCocktailSaveRequest {

    @NotBlank(message = "鸡尾酒名称不能为空")
    @Size(max = 100, message = "鸡尾酒名称最长100位")
    private String name;

    private String description;

    @Size(max = 255, message = "图片地址最长255位")
    private String imageUrl;

    @Min(value = 0, message = "酒精度不能小于0")
    @Max(value = 100, message = "酒精度不能大于100")
    private Integer alcoholLevel;

    private String steps;

    @Valid
    @NotNull(message = "材料列表不能为空")
    private List<MaterialItemRequest> materials = new ArrayList<>();

    @Data
    public static class MaterialItemRequest {

        @NotNull(message = "材料ID不能为空")
        private Long materialId;

        @NotBlank(message = "用量不能为空")
        @Size(max = 50, message = "用量最长50位")
        private String amount;
    }
}
