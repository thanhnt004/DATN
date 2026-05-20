package com.example.searchservice.domain.event;

import lombok.*;
import model.SpecAttribute;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Event published by product-service on Kafka topic "product-events".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {

    public enum EventType {
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        PRODUCT_DELETED,
        PRODUCT_ACTIVATED,
        PRODUCT_DEACTIVATED
    }

    private EventType eventType;
    private String productId;
    private String sellerId;
    private String categoryId;
    private String name;
    private String slug;
    private String description;
    private String status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer soldCount;
    private String thumbnailUrl;
    private List<SpecAttribute> specifications;
    private List<String> optionNames;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isDeleted;
}
