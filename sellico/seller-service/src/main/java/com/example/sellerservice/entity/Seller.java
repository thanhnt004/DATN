package com.example.sellerservice.entity;

import com.example.sellerservice.entity.enums.SellerStatus;
import com.example.sellerservice.entity.enums.SellerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "shop_slug", nullable = false, unique = true, length = 100)
    private String shopSlug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "seller_type", nullable = false)
    @Builder.Default
    private SellerType sellerType = SellerType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    // Contact Information
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    // Address
    @Column(name = "address")
    private String address;

    @Column(name = "ward")
    private String ward;

    @Column(name = "district")
    private String district;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    @Builder.Default
    private String country = "Vietnam";

    // Business Information (for BUSINESS type)
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "business_license_number")
    private String businessLicenseNumber;

    // Statistics
    @Column(name = "rating_avg", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "total_products")
    @Builder.Default
    private Integer totalProducts = 0;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "follower_count")
    @Builder.Default
    private Integer followerCount = 0;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "status_note", columnDefinition = "TEXT")
    private String statusNote;

    @Column(name = "status_updated_by")
    private UUID statusUpdatedBy;

    // Relationships
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SellerDocument> documents = new ArrayList<>();

    @OneToOne(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerBankAccount bankAccount;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.shopSlug == null && this.shopName != null) {
            this.shopSlug = slugify(this.shopName);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    private String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // Business methods
    public boolean isActive() {
        return status == SellerStatus.ACTIVE;
    }

    public boolean canSell() {
        return status == SellerStatus.ACTIVE;
    }

    public void activate() {
        this.status = SellerStatus.ACTIVE;
        this.approvedAt = Instant.now();
        this.rejectionReason = null;
        this.rejectedAt = null;
    }

    public void reject(String reason, UUID adminId) {
        this.status = SellerStatus.REJECTED;
        this.rejectionReason = reason;
        this.rejectedAt = Instant.now();
        this.statusUpdatedBy = adminId;
    }

    public void suspend(String reason) {
        this.status = SellerStatus.SUSPENDED;
        this.statusNote = reason;
    }

    public void ban() {
        this.status = SellerStatus.BANNED;
    }

    public void reactivate() {
        this.status = SellerStatus.ACTIVE;
        this.statusNote = null;
    }

    public void close() {
        this.status = SellerStatus.CLOSED;
    }

    public boolean canResubmit() {
        return this.status == SellerStatus.REJECTED;
    }

    public void incrementProductCount() {
        this.totalProducts++;
    }

    public void decrementProductCount() {
        if (this.totalProducts > 0) {
            this.totalProducts--;
        }
    }

    public void incrementOrderCount() {
        this.totalOrders++;
    }

    public void addRevenue(BigDecimal amount) {
        this.totalRevenue = this.totalRevenue.add(amount);
    }
}

