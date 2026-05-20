package com.example.discountservice.adapter.out.persistence.repository;

import com.example.discountservice.adapter.out.persistence.entity.CouponUsageHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CouponUsageHistoryJpaRepository extends JpaRepository<CouponUsageHistoryEntity, UUID> {
    void deleteByOrderId(UUID orderId);
    boolean existsByCouponIdAndOrderId(UUID couponId, UUID orderId);
}

