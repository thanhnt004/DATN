package com.example.productservice.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOptionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "seller_id")
    private UUID sellerId; // null = ADMIN

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String source; // ADMIN / SELLER

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @org.hibernate.annotations.BatchSize(size = 20)
    @Builder.Default
    private List<ProductOptionValueEntity> values = new ArrayList<>();

    /** NULL when used as a standalone template */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void addValue(ProductOptionValueEntity value) {
        values.add(value);
        value.setOption(this);
    }
}

