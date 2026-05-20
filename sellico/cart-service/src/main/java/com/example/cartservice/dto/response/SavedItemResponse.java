package com.example.cartservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedItemResponse {
    private UUID id;
    private UUID skuId;
    private UUID productId;
    private Instant createdAt;

    // Enriched data (optional)
    private String productName;
    private String imageUrl;
    private String price;
}

