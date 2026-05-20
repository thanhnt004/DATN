package com.example.discountservice.adapter.out.persistence.repository;

import com.example.discountservice.adapter.out.persistence.entity.UserCouponClaimEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCouponClaimJpaRepository extends JpaRepository<UserCouponClaimEntity, UUID> {

    Optional<UserCouponClaimEntity> findByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, String status);

    List<UserCouponClaimEntity> findByUserIdAndStatusOrderByClaimedAtDesc(UUID userId, String status, Pageable pageable);

    long countByCouponIdAndUserId(UUID couponId, UUID userId);

    boolean existsByCouponIdAndUserIdAndStatus(UUID couponId, UUID userId, String status);
}

