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
    private String nameEn;
    private String imageUrl;
    private String imageUrlThumb;
    private String imageUrlCard;
    private String imageUrlDetail;
    private String source;
    private String sourceId;

    public static MaterialResponse from(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .name(material.getName())
                .category(material.getCategory())
                .nameEn(material.getNameEn())
                .imageUrl(material.getImageUrl())
                .source(material.getSource())
                .sourceId(material.getSourceId())
                .build();
    }
}
