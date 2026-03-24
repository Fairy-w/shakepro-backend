package com.shakepro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileRecordRequest {

    @NotBlank(message = "objectKey不能为空")
    private String objectKey;

    @NotBlank(message = "url不能为空")
    private String url;

    private String contentType;
    private Long size;
}
