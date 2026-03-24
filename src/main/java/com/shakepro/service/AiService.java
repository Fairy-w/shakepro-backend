package com.shakepro.service;

import com.shakepro.dto.request.AiRecommendRequest;
import com.shakepro.dto.response.AiRecommendResponse;

import java.util.List;

public interface AiService {

    List<AiRecommendResponse> recommend(AiRecommendRequest request);
}
