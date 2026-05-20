package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.enums.SellerStatus;
import com.example.sellerservice.entity.enums.SellerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
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
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer totalProducts;
    private Integer totalOrders;
    private Integer followerCount;
    private Instant createdAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private String statusNote;
}

