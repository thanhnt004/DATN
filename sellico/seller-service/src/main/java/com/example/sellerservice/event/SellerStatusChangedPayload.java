package com.example.sellerservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload gửi qua Kafka khi trạng thái seller thay đổi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatusChangedPayload {
    private String sellerId;
    private String userId;
    private String email;
    private String shopName;
    private String newStatus;
    private String reason;
}
