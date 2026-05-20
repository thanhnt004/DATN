package com.example.categoryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "category_product_mapping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CategoryProductMappingId.class) // composite primary key
public class CategoryProductMapping {
    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "is_main")
    @Builder.Default
    private Boolean isMain = false;
}

// Lớp hỗ trợ Composite Key
@Data
@NoArgsConstructor
@AllArgsConstructor
class CategoryProductMappingId implements java.io.Serializable {
    private UUID categoryId;
    private UUID productId;
}