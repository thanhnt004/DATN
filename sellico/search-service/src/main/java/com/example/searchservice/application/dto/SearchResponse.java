package com.example.searchservice.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    private String id;
    private String name;
    private String slug;
    private String thumbnailUrl;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double ratingAvg;
    private Integer ratingCount;
    private Integer soldCount;
    private String categoryId;
    private String sellerId;
    private String status;
    private Instant createdAt;
}
