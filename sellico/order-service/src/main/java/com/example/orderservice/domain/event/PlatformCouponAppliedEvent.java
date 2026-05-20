package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Emitted once per checkout when a platform-level coupon is consumed.
 * Carries the per-order pro-rated allocations so discount-service can:
 *   - mark the user_coupon_claim as USED exactly once
 *   - increment coupon.used_quantity by 1
 *   - persist one coupon_usage_history row per (orderId, share)
 */
@Getter
public class PlatformCouponAppliedEvent extends DomainEvent {

    private final UUID couponId;
    private final UUID userId;
    private final BigDecimal totalDiscount;
    private final List<OrderAllocation> allocations;

    @Builder
    public PlatformCouponAppliedEvent(UUID couponId, UUID userId,
                                      BigDecimal totalDiscount,
                                      List<OrderAllocation> allocations) {
        super();
        this.couponId = couponId;
        this.userId = userId;
        this.totalDiscount = totalDiscount;
        this.allocations = allocations;
    }

    @Override
    public UUID getAggregateId() {
        return couponId;
    }

    @Override
    public String getAggregateType() {
        return "PlatformCoupon";
    }

    @Getter
    @Builder
    public static class OrderAllocation {
        private UUID orderId;
        private BigDecimal share;
    }
}
