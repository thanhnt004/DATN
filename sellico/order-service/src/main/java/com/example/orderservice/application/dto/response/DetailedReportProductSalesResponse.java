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
public class DetailedReportProductSalesResponse {
    private OrderPageResponse<ProductSalesData> productsPage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSalesData {
        private String productId;
        private String productName;
        private String imageUrl;
        private int totalQuantitySold;
        private BigDecimal totalRevenue;
    }
}
