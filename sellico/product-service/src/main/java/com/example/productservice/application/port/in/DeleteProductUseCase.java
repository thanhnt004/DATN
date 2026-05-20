package com.example.productservice.application.port.in;

import java.util.UUID;

public interface DeleteProductUseCase {
    /**
     * Soft delete a product
     */
    void deleteProduct(UUID productId, UUID sellerId);
}
