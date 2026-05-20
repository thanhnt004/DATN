package com.example.discountservice.application.port.out;

import com.example.discountservice.domain.model.DiscountRule;

import java.util.List;
import java.util.UUID;

public interface DiscountRuleRepositoryPort {
    List<DiscountRule> saveAll(List<DiscountRule> rules);
    List<DiscountRule> findByCouponId(UUID couponId);
    void deleteByCouponId(UUID couponId);
}

