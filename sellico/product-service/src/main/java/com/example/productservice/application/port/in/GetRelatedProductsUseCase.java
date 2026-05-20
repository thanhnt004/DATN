package com.example.productservice.application.port.in;

import com.example.productservice.domain.model.Product;

import java.util.List;
import java.util.UUID;

public interface GetRelatedProductsUseCase {
    /**
     * Get related/similar products based on same category
     * Used on product detail page: "Sản phẩm tương tự"
     */
    List<Product> getRelatedProducts(UUID productId, int limit);
}

