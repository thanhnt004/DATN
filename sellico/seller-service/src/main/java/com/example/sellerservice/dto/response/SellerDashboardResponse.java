package com.example.sellerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dashboard statistics for seller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponse {
    private UUID sellerId;
    private Integer totalProducts;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private Integer followerCount;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer pendingOrders;
    private Integer processingOrders;
    private Integer lowStockProducts;
}

