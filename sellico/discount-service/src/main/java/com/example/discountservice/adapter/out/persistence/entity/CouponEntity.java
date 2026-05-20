package com.example.discountservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discount_coupons")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CouponEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID campaignId;
    private UUID sellerId;
    @Column(unique = true) private String code;
    private String couponType;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalQuantity;
    private Integer claimedQuantity;
    private Integer usedQuantity;
    private Integer maxUsagePerUser;
    private Boolean isStackable;
    private String status;
    private Instant startDate;
    private Instant endDate;
    @CreationTimestamp @Column(updatable = false) private Instant createdAt;
    @UpdateTimestamp private Instant updatedAt;
    @Version private Integer version;
}

