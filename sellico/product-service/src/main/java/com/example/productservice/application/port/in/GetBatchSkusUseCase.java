package com.example.productservice.application.port.in;

import com.example.productservice.domain.model.ProductSku;

import java.util.List;
import java.util.UUID;

public interface GetBatchSkusUseCase {
    /**
     * Get multiple SKUs by their IDs (for internal service communication)
     * Used by: Cart Service, Order Service for validation
     */
    List<ProductSku> getBatchSkusByIds(List<UUID> skuIds);

    /**
     * Get multiple SKUs by their codes (for internal service communication)
     */
    List<ProductSku> getBatchSkusByCodes(List<String> skuCodes);
}

