package com.example.discountservice.domain.model;

import com.example.discountservice.domain.model.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRule {
    private UUID id;
    private UUID couponId;
    private RuleType ruleType;
    private UUID targetId;
}

