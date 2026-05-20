package com.example.sellerservice.dto.response;

import com.example.sellerservice.entity.enums.SellerStatus;
import com.example.sellerservice.entity.enums.SellerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight seller response for public/listing purposes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerSummaryResponse {
    private UUID id;
    private String shopName;
    private String shopSlug;
    private String logoUrl;
    private String city;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer totalProducts;
    private Integer followerCount;
    private Boolean isFollowing;
}

