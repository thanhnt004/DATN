package com.example.orderservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_fee_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_fee_rate", nullable = false)
    private BigDecimal paymentFeeRate;

    @Column(name = "commission_fee_rate", nullable = false)
    private BigDecimal commissionFeeRate;

    @Column(name = "service_fee_rate", nullable = false)
    private BigDecimal serviceFeeRate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
