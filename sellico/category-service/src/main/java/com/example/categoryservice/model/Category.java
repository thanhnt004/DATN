package com.example.categoryservice.model;

import com.example.categoryservice.dto.response.CategoryBreadcrumb;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;// unique within the same parent, generated from name

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    private Integer level = 1;

    @Column(nullable = false)
    private String path;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private String status = "ACTIVE";//ACTIVE | INACTIVE | DELETED

    @Column(name = "product_count")
    @Builder.Default
    private Integer productCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relationships
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryAttribute> attributes;

    public void addAttribute(CategoryAttribute attribute) {
        attributes.add(attribute);
        attribute.setCategory(this);
    }
    public void removeAttribute(CategoryAttribute attribute) {
        attributes.remove(attribute);
    }

}