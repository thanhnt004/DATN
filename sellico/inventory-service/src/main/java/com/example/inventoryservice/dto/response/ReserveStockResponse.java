package com.example.inventoryservice.dto.response;

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
public class ReserveStockResponse {
    private UUID orderId;
    private Boolean success;
    private List<ReservationResponse> reservations;
    private List<FailedReservation> failedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedReservation {
        private UUID skuId;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private String reason;
    }
}

