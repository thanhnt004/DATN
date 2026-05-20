package com.example.discountservice.application.port.out;

import com.example.discountservice.domain.model.UserCouponClaim;
import com.example.discountservice.domain.model.enums.ClaimStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCouponClaimRepositoryPort {
    UserCouponClaim save(UserCouponClaim claim);
    Optional<UserCouponClaim> findById(UUID id);
    Optional<UserCouponClaim> findByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, ClaimStatus status);
    List<UserCouponClaim> findByUserId(UUID userId, ClaimStatus status, int page, int size);
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
    boolean existsByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, ClaimStatus status);
}

