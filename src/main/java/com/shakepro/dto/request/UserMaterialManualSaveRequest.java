package com.shakepro.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserMaterialManualSaveRequest {

    @NotNull(message = "materialId不能为空")
    private Long materialId;

    @Size(max = 32, message = "source长度不能超过32")
    private String source;

    @Size(max = 120, message = "brand长度不能超过120")
    private String brand;

    @Size(max = 64, message = "categoryId长度不能超过64")
    private String categoryId;

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
