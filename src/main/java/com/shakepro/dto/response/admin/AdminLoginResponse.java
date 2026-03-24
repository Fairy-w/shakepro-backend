package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponse {

    private String token;
    private String tokenType;
    private Long expireSeconds;
    private AdminProfileResponse user;
}
