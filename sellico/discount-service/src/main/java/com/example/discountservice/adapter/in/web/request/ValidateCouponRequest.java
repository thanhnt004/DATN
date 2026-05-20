package com.example.discountservice.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ValidateCouponRequest {
    @NotNull private UUID couponId;
    @NotNull private UUID userId;
    @NotNull private BigDecimal orderAmount;
    private  UUID sellerId;
}

