package com.example.searchservice.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String keyword;

    private String categoryId;

    /** When provided, matches products in ANY of these category IDs (includes subcategories). */
    private List<String> categoryIds;

    private String sellerId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private BigDecimal minRating;

    private String status;

    /**
     * Sort field: "relevance" | "price_asc" | "price_desc" | "sold_desc" | "rating_desc" | "newest"
     */
    private String sortBy = "relevance";

    private int page = 0;

    private int size = 20;
}
