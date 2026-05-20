package com.example.productservice.application.port.in;

import com.example.productservice.domain.model.Product;

import java.util.UUID;

public interface GetProductUseCase {
    /**
     * Get product by ID
     */
    Product getProductById(UUID productId);

    /**
     * Get product by slug
     */
    Product getProductBySlug(String slug);
}
