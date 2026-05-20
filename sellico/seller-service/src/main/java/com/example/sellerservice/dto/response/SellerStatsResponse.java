package com.example.sellerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin overview statistics of seller counts by status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatsResponse {
    private long totalSellers;
    private long pendingSellers;
    private long activeSellers;
    private long rejectedSellers;
    private long suspendedSellers;
    private long bannedSellers;
    private long closedSellers;
}

