package com.example.orderservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedReportTrendResponse {
    private List<RevenueDataPoint> revenueByTime;
    private List<RevenueDataPoint> previousRevenueByTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueDataPoint {
        private String period;
        private BigDecimal revenue;
        private long orderCount;
    }
}
