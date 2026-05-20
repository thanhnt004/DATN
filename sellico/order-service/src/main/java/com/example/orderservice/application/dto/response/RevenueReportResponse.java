package com.example.orderservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {
    private List<RevenueDataPoint> dataPoints;
    private BigDecimal totalRevenueInRange;
    private long totalOrdersInRange;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueDataPoint {
        private String period; // e.g., "2024-03-01" or "2024-03"
        private BigDecimal revenue;
        private long orderCount;
    }
}
