package org.example.notificationservice.infrastructure.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seller event payload DTOs
 */
public class SellerEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerStatusChangedPayload {
        private String sellerId;
        private String userId;
        private String email;
        private String shopName;
        private String newStatus;
        private String reason;
    }
}
