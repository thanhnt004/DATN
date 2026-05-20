package com.example.discountservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_coupon_claims")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class UserCouponClaimEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID couponId;
    private UUID userId;
    private String status;
    private Instant claimedAt;
    private Instant usedAt;
}

