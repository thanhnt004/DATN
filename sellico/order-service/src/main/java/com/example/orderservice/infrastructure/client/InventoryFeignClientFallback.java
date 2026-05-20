package com.example.orderservice.infrastructure.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Component
public class InventoryFeignClientFallback implements InventoryFeignClient {

    @Override
    public ApiResponse<ReserveStockResponse> reserveStock(ReserveStockRequest request) {
        return null;
    }

    @Override
    public ApiResponse<Void> confirmReservation(UUID orderId) {
        return null;
    }

    @Override
    public ApiResponse<List<StockAvailabilityResponse>> getInventories(List<UUID> skuIds) {
        return null;
    }

    @Override
    public ApiResponse<Void> cancelReservation(UUID orderId) {
        return null;
    }
}

