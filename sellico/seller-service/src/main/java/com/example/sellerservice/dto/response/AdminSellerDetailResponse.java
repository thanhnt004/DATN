package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.enums.SellerStatus;
import com.example.sellerservice.entity.enums.SellerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full seller detail for admin — includes documents and bank account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSellerDetailResponse {
    private UUID id;
    private UUID userId;
    private String shopName;
    private String shopSlug;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private SellerType sellerType;
    private SellerStatus status;
    private String email;
    private String phone;
    private String address;
    private String ward;
    private String district;
    private String city;
    private String country;
    private String businessName;
    private String taxCode;
    private String businessLicenseNumber;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer totalProducts;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private Integer followerCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private String statusNote;
    private UUID statusUpdatedBy;

    private List<SellerDocumentResponse> documents;
    private BankAccountResponse bankAccount;
}

