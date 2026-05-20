package com.example.discountservice.domain.model;

import com.example.discountservice.domain.model.enums.CouponStatus;
import com.example.discountservice.domain.model.enums.CouponType;
import com.example.discountservice.domain.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private UUID id;
    private UUID campaignId;
    private UUID sellerId;
    private String code;
    private CouponType couponType;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer usedQuantity;
    private Integer maxUsagePerUser;
    private Boolean isStackable;
    private CouponStatus status;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer version;
    private List<DiscountRule> rules;

    public boolean isExpired() {
        return Instant.now().isAfter(endDate);
    }

    public boolean isNotStarted() {
        return Instant.now().isBefore(startDate);
    }

    public boolean isActive() {
        return status == CouponStatus.ACTIVE && !isExpired() && !isNotStarted();
    }

    public boolean isDepleted() {
        return totalQuantity > 0 && claimedQuantity >= totalQuantity;
    }

    /**
     * Calculate discount for given order amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount.compareTo(minOrderAmount) < 0) return BigDecimal.ZERO;

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }

        // Discount cannot exceed order amount
        return discount.min(orderAmount);
    }
}

