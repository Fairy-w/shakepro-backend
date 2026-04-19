package com.shakepro.dto.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPageBatchImportRequest {

    @NotBlank(message = "列表页网址不能为空")
    @Size(max = 1000, message = "列表页网址长度不能超过1000个字符")
    private String listUrl;

    @Min(value = 1, message = "最大抓取条数不能小于1")
    @Max(value = 1000, message = "最大抓取条数不能超过1000")
    private Integer maxItems = 50;

    @Min(value = 1, message = "并发数不能小于1")
    @Max(value = 8, message = "并发数不能超过8")
    private Integer concurrency = 3;

    private Boolean autoGenerate = Boolean.FALSE;

    private Boolean autoSave = Boolean.FALSE;

    private Boolean onlyNew = Boolean.TRUE;
}
