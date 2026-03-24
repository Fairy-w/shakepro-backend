package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BannerResponse {

    private Long cocktailId;
    private String imageUrl;
    private String title;
}
