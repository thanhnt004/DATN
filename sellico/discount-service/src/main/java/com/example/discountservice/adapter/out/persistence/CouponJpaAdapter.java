package com.example.discountservice.adapter.out.persistence;

import com.example.discountservice.adapter.out.persistence.entity.CouponEntity;
import com.example.discountservice.adapter.out.persistence.repository.CouponJpaRepository;
import com.example.discountservice.application.port.out.CouponRepositoryPort;
import com.example.discountservice.domain.model.Coupon;
import com.example.discountservice.domain.model.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CouponJpaAdapter implements CouponRepositoryPort {

    private final CouponJpaRepository repo;

    @Override
    public Coupon save(Coupon c) { return toDomain(repo.save(toEntity(c))); }

    @Override
    public Optional<Coupon> findById(UUID id) { return repo.findById(id).map(this::toDomain); }

    @Override
    public Optional<Coupon> findByCode(String code) { return repo.findByCode(code).map(this::toDomain); }

    @Override
    public boolean existsByCode(String code) { return repo.existsByCode(code); }

    @Override
    public Page<Coupon> findBySellerId(UUID sellerId, CouponStatus status, int page, int size) {
        return repo.searchBySeller(sellerId, status != null ? status.name() : null, PageRequest.of(page, size)).map(this::toDomain);
    }

    @Override
    public Page<Coupon> findByType(String couponType, CouponStatus status, int page, int size) {
        return repo.searchByType(couponType, status != null ? status.name() : null, PageRequest.of(page, size)).map(this::toDomain);
    }

    @Override
    public Page<Coupon> findAllActive(int page, int size) {
        return repo.findAllActive(PageRequest.of(page, size)).map(this::toDomain);
    }

    @Override
    public Page<Coupon> findActiveBySellerIds(UUID sellerId, int page, int size) {
        return repo.findActiveBySellerId(sellerId, PageRequest.of(page, size)).map(this::toDomain);
    }

    @Override
    public int incrementClaimedQuantity(UUID couponId) { return repo.incrementClaimedQuantity(couponId); }

    @Override
    public int incrementUsedQuantity(UUID couponId) { return repo.incrementUsedQuantity(couponId); }

    @Override
    public int decrementUsedQuantity(UUID couponId) { return repo.decrementUsedQuantity(couponId); }

    private CouponEntity toEntity(Coupon c) {
        return CouponEntity.builder()
                .id(c.getId()).campaignId(c.getCampaignId()).sellerId(c.getSellerId())
                .code(c.getCode())
                .couponType(c.getCouponType() != null ? c.getCouponType().name() : null)
                .discountType(c.getDiscountType() != null ? c.getDiscountType().name() : null)
                .discountValue(c.getDiscountValue()).minOrderAmount(c.getMinOrderAmount())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .totalQuantity(c.getTotalQuantity()).claimedQuantity(c.getClaimedQuantity())
                .usedQuantity(c.getUsedQuantity()).maxUsagePerUser(c.getMaxUsagePerUser())
                .isStackable(c.getIsStackable()).status(c.getStatus() != null ? c.getStatus().name() : null)
                .startDate(c.getStartDate()).endDate(c.getEndDate()).version(c.getVersion())
                .build();
    }

    private Coupon toDomain(CouponEntity e) {
        return Coupon.builder()
                .id(e.getId()).campaignId(e.getCampaignId()).sellerId(e.getSellerId())
                .code(e.getCode())
                .couponType(e.getCouponType() != null ? CouponType.valueOf(e.getCouponType()) : null)
                .discountType(e.getDiscountType() != null ? DiscountType.valueOf(e.getDiscountType()) : null)
                .discountValue(e.getDiscountValue()).minOrderAmount(e.getMinOrderAmount())
                .maxDiscountAmount(e.getMaxDiscountAmount())
                .totalQuantity(e.getTotalQuantity()).claimedQuantity(e.getClaimedQuantity())
                .usedQuantity(e.getUsedQuantity()).maxUsagePerUser(e.getMaxUsagePerUser())
                .isStackable(e.getIsStackable())
                .status(e.getStatus() != null ? CouponStatus.valueOf(e.getStatus()) : null)
                .startDate(e.getStartDate()).endDate(e.getEndDate())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).version(e.getVersion())
                .build();
    }
}

