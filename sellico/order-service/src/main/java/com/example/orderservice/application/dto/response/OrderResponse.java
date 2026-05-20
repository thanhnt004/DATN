package com.example.orderservice.application.dto.response;

import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.example.orderservice.domain.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private UUID sellerId;
    private OrderStatus status;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Shipping Address
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;

    // Payment
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentUrl;
    private Instant paidAt;

    // Shipping
    private String shippingProvider;
    private String trackingNumber;
    private Instant shippedAt;
    private Instant deliveredAt;

    // Notes
    private String buyerNote;
    private String sellerNote;
    private String cancelReason;
    private String failureReason;
    private String voucherCode;

    // Platform voucher allocated to this order (pro-rated by subtotal)
    private UUID platformCouponId;
    private BigDecimal platformVoucherShare;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    // Items
    private List<OrderItemResponse> items;
}

