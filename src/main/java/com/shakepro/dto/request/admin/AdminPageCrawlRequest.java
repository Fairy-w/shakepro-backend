package com.shakepro.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPageCrawlRequest {

    @NotBlank(message = "网址不能为空")
    @Size(max = 1000, message = "网址长度不能超过1000个字符")
    private String url;
}
