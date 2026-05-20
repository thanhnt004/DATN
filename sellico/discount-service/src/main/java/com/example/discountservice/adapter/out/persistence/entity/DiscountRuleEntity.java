package com.example.discountservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "discount_rules")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DiscountRuleEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID couponId;
    private String ruleType;
    private UUID targetId;
}

