package com.example.discountservice.application.port.out;

import com.example.discountservice.domain.model.CouponUsageHistory;

import java.util.UUID;

public interface CouponUsageHistoryRepositoryPort {
    CouponUsageHistory save(CouponUsageHistory history);
    void deleteByOrderId(UUID orderId);
    boolean existsByCouponIdAndOrderId(UUID couponId, UUID orderId);
}

