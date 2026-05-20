package com.example.discountservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_usage_history")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CouponUsageHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID claimId;
    private UUID couponId;
    private UUID userId;
    private UUID orderId;
    private BigDecimal discountAmount;
    private Instant usedAt;
}

