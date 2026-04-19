package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserMaterialResponse {

    private Long id;
    private Long userId;
    private String barcode;
    private Long materialId;
    private String name;
    private String brand;
    private String categoryId;
    private String source;
    private String imageUrl;
    private String imageUrlThumb;
    private String imageUrlCard;
    private String imageUrlDetail;
    // Temporarily disabled: reserved for future capacity management.
    private String capacityText;
    // Temporarily disabled: reserved for future capacity management.
    private String remainLevel;
    // Temporarily disabled: reserved for future capacity management.
    private Boolean opened;
    private Boolean hasItem;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
