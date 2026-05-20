package com.example.sellerservice.controller;

import com.example.sellerservice.dto.response.SellerResponse;
import com.example.sellerservice.dto.response.SellerSummaryResponse;
import com.example.sellerservice.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for internal service communication
 * Base path: /internal/v1
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalSellerController {

    private final SellerService sellerService;

    /**
     * GET /internal/v1/sellers/batch - Get batch sellers
     */
    @GetMapping("/sellers/batch")
    public ResponseEntity<ApiResponse<List<SellerSummaryResponse>>> getBatchSellers(
            @RequestParam("ids") List<UUID> ids
    ) {
        List<SellerSummaryResponse> response = sellerService.getBatchSellers(ids);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/sellers/batch - Get batch sellers (POST version)
     */
    @PostMapping("/sellers/batch")
    public ResponseEntity<ApiResponse<List<SellerSummaryResponse>>> getBatchSellersPost(
            @RequestBody List<UUID> ids
    ) {
        List<SellerSummaryResponse> response = sellerService.getBatchSellers(ids);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/sellers/{sellerId} - Get seller by ID
     */
    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<ApiResponse<SellerResponse>> getSeller(@PathVariable("sellerId") UUID sellerId) {
        SellerResponse response = sellerService.getSellerById(sellerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/sellers/{sellerId}/active - Check if seller is active
     */
    @GetMapping("/sellers/{sellerId}/active")
    public ResponseEntity<ApiResponse<Boolean>> isSellerActive(@PathVariable("sellerId") UUID sellerId) {
        boolean active = sellerService.isSellerActive(sellerId);
        return ResponseEntity.ok(ApiResponse.success(active));
    }

    /**
     * GET /internal/v1/sellers/user/{userId} - Get seller by user ID
     */
    @GetMapping("/sellers/user/{userId}")
    public ResponseEntity<ApiResponse<SellerResponse>> getSellerByUserId(@PathVariable("userId") UUID userId) {
        SellerResponse response = sellerService.getSellerByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PATCH /internal/v1/sellers/{sellerId}/stats - Update seller stats
     */
    @PatchMapping("/sellers/{sellerId}/stats")
    public ResponseEntity<ApiResponse<Void>> updateSellerStats(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "productCount", required = false) Integer productCount,
            @RequestParam(value = "orderCount", required = false) Integer orderCount
    ) {
        sellerService.updateSellerStats(sellerId, productCount, orderCount);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

