package com.example.cartservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuInfo {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID sellerId;
    private String skuCode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private String status;
    private String imageUrl;
    private Map<String, String> attributes; // variant attributes
}

