package com.shakepro.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPageFieldExtractRequest {

    @NotBlank(message = "来源网址不能为空")
    @Size(max = 1000, message = "来源网址长度不能超过1000个字符")
    private String url;

    @Size(max = 300, message = "页面标题长度不能超过300个字符")
    private String title;

    @NotBlank(message = "HTML原文不能为空")
    @Size(max = 2000000, message = "HTML原文长度不能超过2000000个字符")
    private String html;
}
