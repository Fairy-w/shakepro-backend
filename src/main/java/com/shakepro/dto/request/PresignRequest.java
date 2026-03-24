package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PresignRequest {

    @NotBlank(message = "文件名不能为空")
    private String filename;

    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @Positive(message = "文件大小必须大于0")
    private Long size;
}
