package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.response.AdminOrderStatsResponse;
import com.example.orderservice.application.dto.response.DetailedReportSummaryResponse;
import com.example.orderservice.application.dto.response.DetailedReportTrendResponse;
import com.example.orderservice.application.dto.response.DetailedReportCategoryResponse;
import com.example.orderservice.application.dto.response.DetailedReportProductSalesResponse;
import com.example.orderservice.application.dto.response.RevenueReportResponse;

import java.time.Instant;
import java.util.UUID;

public interface SellerReportUseCase {
    AdminOrderStatsResponse getOverallStats(UUID sellerId);

    RevenueReportResponse getRevenueReport(UUID sellerId, Instant startDate, Instant endDate, String periodType);

    DetailedReportSummaryResponse getDetailedReportSummary(UUID sellerId, Instant startDate, Instant endDate, String periodType);

    DetailedReportTrendResponse getDetailedReportTrend(UUID sellerId, Instant startDate, Instant endDate, String periodType);

    DetailedReportCategoryResponse getDetailedReportCategory(UUID sellerId, Instant startDate, Instant endDate, String periodType);

    DetailedReportProductSalesResponse getDetailedReportProducts(UUID sellerId, Instant startDate, Instant endDate, String periodType,
                                                                 int page, int size, String sortBy, String sortDir);
}
