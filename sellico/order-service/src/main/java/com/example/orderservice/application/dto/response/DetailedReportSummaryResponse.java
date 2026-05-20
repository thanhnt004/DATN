package com.example.orderservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedReportSummaryResponse {
    private BigDecimal totalGrossSales;
    private BigDecimal netRevenue;
    private long totalOrderCount;
    private double cancellationRate;
    private double returnRate;
    private BigDecimal averageOrderValue;
    private WaterfallChartData waterfallChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterfallChartData {
        private BigDecimal baseProductRevenue;
        private BigDecimal shopVouchers;
        private BigDecimal shippingSubsidies;
        private BigDecimal platformFees;
        private BigDecimal paymentFees;
        private BigDecimal commissionFees;
        private BigDecimal serviceFees;
        private BigDecimal payout;
    }
}
