package com.example.productservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed SKU response for internal service communication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuDetailResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID sellerId;
    private String skuCode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String status;
    private String imageUrl;
    private Integer weightGram;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;

    /** Variant attributes, e.g. {"Color": "Red", "Size": "XL"} */
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();
}

