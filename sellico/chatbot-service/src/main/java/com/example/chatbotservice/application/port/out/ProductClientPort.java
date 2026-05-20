package com.example.chatbotservice.application.port.out;

import com.example.chatbotservice.domain.model.ProductInfo;

import java.util.List;

/**
 * Port for fetching product data from the product-service.
 */
public interface ProductClientPort {

    /**
     * Search products by keyword.
     */
    List<ProductInfo> searchProducts(String keyword, int page, int size);

    /**
     * Get top/popular products.
     */
    List<ProductInfo> getTopProducts(int limit);

    /**
     * Get products by category slug.
     */
    List<ProductInfo> getProductsByCategory(String categorySlug, int page, int size);
}
