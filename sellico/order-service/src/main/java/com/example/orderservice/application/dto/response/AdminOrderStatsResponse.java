package com.example.orderservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderStatsResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long completedOrders;
    private long cancelledOrders;
    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByStatus;
}
