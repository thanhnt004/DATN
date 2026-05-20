package com.example.cartservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private UUID id;
    private UUID skuId;
    private UUID productId;
    private UUID sellerId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private Boolean selected;
    private Instant createdAt;

    // Enriched data from product service (optional, filled by client or BFF)
    private String productName;
    private String skuCode;
    private String imageUrl;
    private String sellerName;
    private Integer availableStock;
    private Boolean inStock;
    private Map<String, String> attributes; // variant attributes, e.g. {"Color": "Red"}
}

