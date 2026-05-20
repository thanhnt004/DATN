package com.example.discountservice.adapter.in.web.response;

import com.example.discountservice.domain.model.UserCouponClaim;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserCouponResponse {
    private UUID id;
    private UUID couponId;
    private String status;
    private Instant claimedAt;
    private Instant usedAt;

    // Enriched coupon info
    private String couponCode;
    private String discountType;
    private java.math.BigDecimal discountValue;
    private java.math.BigDecimal minOrderAmount;
    private java.math.BigDecimal maxDiscountAmount;
    private Instant couponEndDate;

    public static UserCouponResponse from(UserCouponClaim claim) {
        return UserCouponResponse.builder()
                .id(claim.getId()).couponId(claim.getCouponId())
                .status(claim.getStatus() != null ? claim.getStatus().name() : null)
                .claimedAt(claim.getClaimedAt()).usedAt(claim.getUsedAt())
                .build();
    }
}

