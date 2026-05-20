package com.example.discountservice.adapter.in.web.response;

import com.example.discountservice.domain.model.Coupon;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponResponse {
    private UUID id;
    private UUID campaignId;
    private UUID sellerId;
    private String code;
    private String couponType;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer usedQuantity;
    private Integer maxUsagePerUser;
    private Boolean isStackable;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;

    // Computed for buyer
    private Integer remainingQuantity;

    public static CouponResponse from(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId()).campaignId(c.getCampaignId()).sellerId(c.getSellerId())
                .code(c.getCode())
                .couponType(c.getCouponType() != null ? c.getCouponType().name() : null)
                .discountType(c.getDiscountType() != null ? c.getDiscountType().name() : null)
                .discountValue(c.getDiscountValue()).minOrderAmount(c.getMinOrderAmount())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .totalQuantity(c.getTotalQuantity()).claimedQuantity(c.getClaimedQuantity())
                .usedQuantity(c.getUsedQuantity()).maxUsagePerUser(c.getMaxUsagePerUser())
                .isStackable(c.getIsStackable())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .startDate(c.getStartDate()).endDate(c.getEndDate()).createdAt(c.getCreatedAt())
                .remainingQuantity(c.getTotalQuantity() - c.getClaimedQuantity())
                .build();
    }
}

