package com.shakepro.dto.response;

import com.shakepro.entity.Material;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterialResponse {

    private Long id;
    private String name;
    private String category;

    public static MaterialResponse from(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .name(material.getName())
                .category(material.getCategory())
                .build();
    }
}
