package com.example.discountservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageHistory {
    private UUID id;
    private UUID claimId;
    private UUID couponId;
    private UUID userId;
    private UUID orderId;
    private BigDecimal discountAmount;
    private Instant usedAt;
}

