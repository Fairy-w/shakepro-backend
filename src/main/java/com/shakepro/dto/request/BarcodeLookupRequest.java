package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BarcodeLookupRequest {

    @NotBlank(message = "条码不能为空")
    @Size(min = 8, max = 32, message = "条码长度必须在8到32之间")
    @Pattern(regexp = "^[0-9\\-\\s]+$", message = "条码只能包含数字、空格或短横线")
    private String barcode;

    @Size(max = 20, message = "locale长度不能超过20")
    private String locale;
}
