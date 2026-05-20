package com.example.discountservice.adapter.out.persistence;

import com.example.discountservice.adapter.out.persistence.entity.DiscountRuleEntity;
import com.example.discountservice.adapter.out.persistence.repository.DiscountRuleJpaRepository;
import com.example.discountservice.application.port.out.DiscountRuleRepositoryPort;
import com.example.discountservice.domain.model.DiscountRule;
import com.example.discountservice.domain.model.enums.RuleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DiscountRuleJpaAdapter implements DiscountRuleRepositoryPort {

    private final DiscountRuleJpaRepository repo;

    @Override
    public List<DiscountRule> saveAll(List<DiscountRule> rules) {
        List<DiscountRuleEntity> entities = rules.stream().map(this::toEntity).toList();
        return repo.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DiscountRule> findByCouponId(UUID couponId) {
        return repo.findByCouponId(couponId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByCouponId(UUID couponId) {
        repo.deleteByCouponId(couponId);
    }

    private DiscountRuleEntity toEntity(DiscountRule r) {
        return DiscountRuleEntity.builder()
                .id(r.getId()).couponId(r.getCouponId())
                .ruleType(r.getRuleType() != null ? r.getRuleType().name() : null)
                .targetId(r.getTargetId()).build();
    }

    private DiscountRule toDomain(DiscountRuleEntity e) {
        return DiscountRule.builder()
                .id(e.getId()).couponId(e.getCouponId())
                .ruleType(e.getRuleType() != null ? RuleType.valueOf(e.getRuleType()) : null)
                .targetId(e.getTargetId()).build();
    }
}

