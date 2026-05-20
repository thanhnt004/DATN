package com.example.shippingservice.client;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {
    private UUID id;
    private UUID sellerId;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private BigDecimal subtotal;

    private String buyerNote;
    private String sellerNote;
    private List<OrderItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemResponse {
        private UUID id;
        private UUID skuId;
        private UUID productId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        COMPLETED,
        CANCELLED
    }
    public enum PaymentMethod {
        COD,
        BANK_TRANSFER,
        MOMO,
        ZALOPAY,
        VNPAY,
        CREDIT_CARD
    }
}
