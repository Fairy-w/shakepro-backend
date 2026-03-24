package com.shakepro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiRecommendResponse {

    private String name;
    private String description;
    private List<String> materials;
    private String steps;
}
