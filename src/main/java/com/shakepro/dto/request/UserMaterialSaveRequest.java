package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserMaterialSaveRequest {

    private Long materialId;

    @Size(max = 32, message = "source长度不能超过32")
    private String source;

    @NotBlank(message = "name不能为空")
    @Size(max = 255, message = "name长度不能超过255")
    private String name;

    @Size(max = 120, message = "brand长度不能超过120")
    private String brand;

    @Size(max = 64, message = "categoryId长度不能超过64")
    private String categoryId;

    @NotBlank(message = "barcode不能为空")
    @Size(min = 8, max = 32, message = "barcode长度必须在8到32之间")
    @Pattern(regexp = "^[0-9\\-\\s]+$", message = "barcode只能包含数字、空格或短横线")
    private String barcode;

    @Size(max = 64, message = "capacityText长度不能超过64")
    // Temporarily disabled: kept for backward compatibility with old clients.
    private String capacityText;

    @Size(max = 32, message = "remainLevel长度不能超过32")
    // Temporarily disabled: kept for backward compatibility with old clients.
    private String remainLevel;

    // Temporarily disabled: kept for backward compatibility with old clients.
    private Boolean opened;
    private Boolean hasItem;

    private List<@Size(max = 32, message = "tag长度不能超过32") String> tags;
}
