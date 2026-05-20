package com.example.discountservice.application.port.out;

import com.example.discountservice.domain.model.Coupon;
import com.example.discountservice.domain.model.enums.CouponStatus;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepositoryPort {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(UUID id);
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);
    Page<Coupon> findBySellerId(UUID sellerId, CouponStatus status, int page, int size);
    Page<Coupon> findByType(String couponType, CouponStatus status, int page, int size);
    Page<Coupon> findAllActive(int page, int size);
    Page<Coupon> findActiveBySellerIds(UUID sellerId, int page, int size);

    /**
     * Atomically increment claimed_quantity and return updated coupon.
     * Uses optimistic locking via version field.
     */
    int incrementClaimedQuantity(UUID couponId);

    /**
     * Atomically increment used_quantity
     */
    int incrementUsedQuantity(UUID couponId);

    /**
     * Atomically decrement used_quantity (for release)
     */
    int decrementUsedQuantity(UUID couponId);
}

