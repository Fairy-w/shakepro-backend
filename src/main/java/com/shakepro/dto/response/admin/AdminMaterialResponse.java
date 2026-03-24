package com.shakepro.dto.response.admin;

import com.shakepro.entity.Material;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminMaterialResponse {

    private Long id;
    private String name;
    private String category;
    private LocalDateTime createdAt;

    public static AdminMaterialResponse from(Material material) {
        return AdminMaterialResponse.builder()
                .id(material.getId())
                .name(material.getName())
                .category(material.getCategory())
                .createdAt(material.getCreatedAt())
                .build();
    }
}
