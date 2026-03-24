package com.shakepro.dto.response.admin;

import com.shakepro.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String role;

    public static AdminProfileResponse from(User user) {
        return AdminProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }
}
