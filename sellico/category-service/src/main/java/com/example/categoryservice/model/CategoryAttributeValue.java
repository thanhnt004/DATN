package com.example.categoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "category_attribute_values")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttributeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id")
    private CategoryAttribute attribute;

    @Column(nullable = false)
    private String value;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
