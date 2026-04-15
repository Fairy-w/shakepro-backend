package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminPageTextResponse {

    private String url;
    private String title;
    private String html;
}
