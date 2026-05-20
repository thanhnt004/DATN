package com.example.productservice.adapter.out.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Minimal DTO for seller-service responses.
 * Only the fields product-service actually needs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private UUID id;       // seller entity PK (the "sellerId" used in products)
    private UUID userId;   // Keycloak user ID (JWT subject)
}
