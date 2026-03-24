package com.shakepro.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminMaterialSaveRequest {

    @NotBlank(message = "材料名称不能为空")
    @Size(max = 100, message = "材料名称最长100位")
    private String name;

    @Size(max = 50, message = "分类最长50位")
    private String category;
}
