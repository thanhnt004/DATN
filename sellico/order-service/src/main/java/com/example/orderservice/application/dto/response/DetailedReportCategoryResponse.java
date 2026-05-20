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
public class DetailedReportCategoryResponse {
    private List<CategoryRevenueData> revenueByCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRevenueData {
        private String categoryName;
        private BigDecimal revenue;
        private long orderCount;
        private long quantitySold;
        private double percentage;
    }
}
