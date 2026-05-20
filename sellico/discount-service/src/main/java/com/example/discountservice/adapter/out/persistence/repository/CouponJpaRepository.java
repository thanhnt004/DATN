package com.example.discountservice.adapter.out.persistence.repository;

import com.example.discountservice.adapter.out.persistence.entity.CouponEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM CouponEntity c WHERE (:sellerId IS NULL OR c.sellerId = :sellerId) AND (:status IS NULL OR c.status = :status) ORDER BY c.createdAt DESC")
    Page<CouponEntity> searchBySeller(@Param("sellerId") UUID sellerId, @Param("status") String status, Pageable pageable);

    @Query("SELECT c FROM CouponEntity c WHERE (:couponType IS NULL OR c.couponType = :couponType) AND (:status IS NULL OR c.status = :status) ORDER BY c.createdAt DESC")
    Page<CouponEntity> searchByType(@Param("couponType") String couponType, @Param("status") String status, Pageable pageable);

    @Query("SELECT c FROM CouponEntity c WHERE c.status = 'ACTIVE' AND c.startDate <= CURRENT_TIMESTAMP AND c.endDate > CURRENT_TIMESTAMP AND c.claimedQuantity < c.totalQuantity ORDER BY c.createdAt DESC")
    Page<CouponEntity> findAllActive(Pageable pageable);

    @Query("SELECT c FROM CouponEntity c WHERE c.status = 'ACTIVE' AND c.sellerId = :sellerId AND c.startDate <= CURRENT_TIMESTAMP AND c.endDate > CURRENT_TIMESTAMP AND c.claimedQuantity < c.totalQuantity ORDER BY c.createdAt DESC")
    Page<CouponEntity> findActiveBySellerId(@Param("sellerId") UUID sellerId, Pageable pageable);

    @Modifying
    @Query("UPDATE CouponEntity c SET c.claimedQuantity = c.claimedQuantity + 1 WHERE c.id = :id AND c.claimedQuantity < c.totalQuantity")
    int incrementClaimedQuantity(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE CouponEntity c SET c.usedQuantity = c.usedQuantity + 1 WHERE c.id = :id")
    int incrementUsedQuantity(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE CouponEntity c SET c.usedQuantity = c.usedQuantity - 1 WHERE c.id = :id AND c.usedQuantity > 0")
    int decrementUsedQuantity(@Param("id") UUID id);
}

