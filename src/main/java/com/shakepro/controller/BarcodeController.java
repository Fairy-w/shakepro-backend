package com.shakepro.controller;

import com.shakepro.common.result.ApiResponse;
import com.shakepro.config.security.SecurityUtils;
import com.shakepro.dto.request.BarcodeLookupRequest;
import com.shakepro.dto.response.BarcodeLookupResponse;
import com.shakepro.service.BarcodeLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Barcodes", description = "条码扫描识别接口")
@RestController
@RequestMapping("/api/barcodes")
@RequiredArgsConstructor
public class BarcodeController {

    private final BarcodeLookupService barcodeLookupService;

    @Operation(summary = "识别条码并返回商品信息")
    @PostMapping("/lookup")
    public ApiResponse<BarcodeLookupResponse> lookup(@Valid @RequestBody BarcodeLookupRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(barcodeLookupService.lookup(userId, request));
    }
}
