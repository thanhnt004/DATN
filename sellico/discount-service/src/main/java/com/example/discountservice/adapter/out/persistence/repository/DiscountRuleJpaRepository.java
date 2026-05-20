package com.example.discountservice.adapter.out.persistence.repository;

import com.example.discountservice.adapter.out.persistence.entity.DiscountRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DiscountRuleJpaRepository extends JpaRepository<DiscountRuleEntity, UUID> {
    List<DiscountRuleEntity> findByCouponId(UUID couponId);
    void deleteByCouponId(UUID couponId);
}

