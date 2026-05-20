package com.example.discountservice.adapter.out.persistence;

import com.example.discountservice.adapter.out.persistence.entity.CouponUsageHistoryEntity;
import com.example.discountservice.adapter.out.persistence.repository.CouponUsageHistoryJpaRepository;
import com.example.discountservice.application.port.out.CouponUsageHistoryRepositoryPort;
import com.example.discountservice.domain.model.CouponUsageHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CouponUsageHistoryJpaAdapter implements CouponUsageHistoryRepositoryPort {

    private final CouponUsageHistoryJpaRepository repo;

    @Override
    public CouponUsageHistory save(CouponUsageHistory h) {
        CouponUsageHistoryEntity entity = CouponUsageHistoryEntity.builder()
                .id(h.getId()).claimId(h.getClaimId()).couponId(h.getCouponId())
                .userId(h.getUserId()).orderId(h.getOrderId())
                .discountAmount(h.getDiscountAmount()).usedAt(h.getUsedAt()).build();
        CouponUsageHistoryEntity saved = repo.save(entity);
        return CouponUsageHistory.builder()
                .id(saved.getId()).claimId(saved.getClaimId()).couponId(saved.getCouponId())
                .userId(saved.getUserId()).orderId(saved.getOrderId())
                .discountAmount(saved.getDiscountAmount()).usedAt(saved.getUsedAt()).build();
    }

    @Override
    @Transactional
    public void deleteByOrderId(UUID orderId) {
        repo.deleteByOrderId(orderId);
    }

    @Override
    public boolean existsByCouponIdAndOrderId(UUID couponId, UUID orderId) {
        return repo.existsByCouponIdAndOrderId(couponId, orderId);
    }
}

