package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignResponse {

    private String uploadUrl;
    private String objectKey;
    private String publicUrl;
    private Integer expireSeconds;
}
