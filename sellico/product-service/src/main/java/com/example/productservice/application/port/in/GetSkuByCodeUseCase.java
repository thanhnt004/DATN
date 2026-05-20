package com.example.productservice.application.port.in;

import com.example.productservice.domain.model.ProductSku;

import java.util.UUID;

public interface GetSkuByCodeUseCase {
    /**
     * Get SKU by its code (for internal service communication)
     */
    ProductSku getSkuByCode(String skuCode);

    /**
     * Get SKU by its code along with product info (for internal service communication)
     */
    record SkuWithProduct(ProductSku sku, UUID productId, String productName, UUID sellerId) {}
    SkuWithProduct getSkuWithProductByCode(String skuCode);
}
