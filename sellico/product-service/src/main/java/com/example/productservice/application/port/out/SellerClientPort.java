package com.example.productservice.application.port.out;

import java.util.UUID;

public interface SellerClientPort {
    boolean isSellerActive(UUID sellerId);

    /**
     * Resolve Keycloak userId (JWT subject) → seller entity PK (sellerId).
     * @throws RuntimeException if no seller found for this userId
     */
    UUID getSellerIdByUserId(UUID userId);
}
