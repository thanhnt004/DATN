package com.example.discountservice.domain.model;

import com.example.discountservice.domain.model.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponClaim {
    private UUID id;
    private UUID couponId;
    private UUID userId;
    private ClaimStatus status;
    private Instant claimedAt;
    private Instant usedAt;
}

