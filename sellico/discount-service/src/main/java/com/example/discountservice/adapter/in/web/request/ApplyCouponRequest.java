package com.example.discountservice.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ApplyCouponRequest {
    @NotNull(message = "Coupon ID is required")
    private UUID couponId;

    @NotNull(message = "Order amount is required")
    @Positive(message = "Order amount must be positive")
    private BigDecimal orderAmount;

    // For internal use (from order-service)
    private UUID orderId;
}

