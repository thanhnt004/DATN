package com.example.productservice.application.port.in;

import com.example.productservice.application.command.GetBatchProductsCommand;
import com.example.productservice.domain.model.Product;

import java.util.List;

public interface GetBatchProductsUseCase {
    /**
     * Get multiple products by their IDs (for internal service communication)
     */
    List<Product> getBatchProducts(GetBatchProductsCommand command);
}

