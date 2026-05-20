package org.example.notificationservice.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Order event payload DTOs
 */
public class OrderEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderCreatedPayload {
        private String orderId;
        private String orderCode;
        private String userId;
        private String email;
        private String customerName;
        private BigDecimal totalAmount;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal discountAmount;
        private String shippingAddress;
        private String paymentMethod;
        private String recipientName;
        private String recipientPhone;
        private List<OrderItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderConfirmedPayload {
        private String orderId;
        private String orderCode;
        private String userId;
        private String email;
        private String customerName;
        private String estimatedDelivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderShippedPayload {
        private String orderId;
        private String orderCode;
        private String userId;
        private String email;
        private String customerName;
        private String trackingNumber;
        private String carrier;
        private String estimatedDelivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDeliveredPayload {
        private String orderId;
        private String orderCode;
        private String userId;
        private String email;
        private String customerName;
        private String deliveredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCancelledPayload {
        private String orderId;
        private String orderCode;
        private String userId;
        private String email;
        private String customerName;
        private String cancelReason;
        private BigDecimal refundAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
        private String imageUrl;
    }
}

