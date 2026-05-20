package com.example.inventoryservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ReservationItem> items;

    /**
     * Reservation duration in minutes (default 15 minutes)
     */
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Builder.Default
    private Integer durationMinutes = 15;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        @NotNull(message = "SKU ID is required")
        private UUID skuId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}

