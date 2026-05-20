package com.example.productservice.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Entity
@Table(name = "product_skus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Column(name = "sku_code", unique = true, nullable = false)
    private String skuCode;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "weight_gram")
    private Integer weightGram;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "sku", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 20)
    @Builder.Default
    private Set<SkuAttributeValue> attributeValues = new HashSet<>();
    public void addAttributeValue(SkuAttributeValue attributeValue) {
        attributeValues.add(attributeValue);
        attributeValue.setSku(this);
    }
}