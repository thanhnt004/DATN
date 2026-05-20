package com.example.productservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight product response for internal service communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String name;
    private String slug;
    private String status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer soldCount;
    private String primaryImageUrl;
}

