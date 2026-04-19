package com.shakepro.service;

import com.shakepro.dto.request.BarcodeLookupRequest;
import com.shakepro.dto.response.BarcodeLookupResponse;

public interface BarcodeLookupService {

    BarcodeLookupResponse lookup(Long userId, BarcodeLookupRequest request);
}
