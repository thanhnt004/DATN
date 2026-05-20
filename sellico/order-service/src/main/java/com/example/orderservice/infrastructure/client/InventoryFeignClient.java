package com.example.orderservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service", fallback = InventoryFeignClientFallback.class)
public interface InventoryFeignClient {

    @PostMapping("/internal/v1/reservations")
    ApiResponse<ReserveStockResponse> reserveStock(@RequestBody ReserveStockRequest request);

    @PostMapping("/internal/v1/reservations/{orderId}/confirm")
    ApiResponse<Void> confirmReservation(@PathVariable("orderId") UUID orderId);

    @PostMapping("/internal/v1/inventories/availability")
    ApiResponse<List<StockAvailabilityResponse>> getInventories(@RequestBody List<UUID> skuIds);

    @PostMapping("/internal/v1/reservations/{orderId}/cancel")
    ApiResponse<Void> cancelReservation(@PathVariable("orderId") UUID orderId);

    record ReserveStockRequest(UUID orderId, List<ReservationItemDto> items, int durationMinutes) {}
    record ReservationItemDto(UUID skuId, int quantity) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ReserveStockResponse {
        private UUID orderId;
        private Boolean success;
        private List<FailedItem> failedItems;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class StockAvailabilityResponse {
        private UUID skuId;
        private Integer availableStock;
        private Boolean isAvailable;
        private Boolean isLowStock;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class FailedItem {
        private UUID skuId;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private String reason;
    }
}

