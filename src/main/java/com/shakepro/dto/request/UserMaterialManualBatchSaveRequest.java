package com.shakepro.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserMaterialManualBatchSaveRequest {

    @NotEmpty(message = "items不能为空")
    private List<@Valid UserMaterialManualSaveRequest> items;
}
