package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateSkuCommand;
import com.example.productservice.domain.model.ProductSku;

public interface UpdateSkuUseCase {
    /**
     * Update a single SKU
     */
    ProductSku updateSku(UpdateSkuCommand command);
}

