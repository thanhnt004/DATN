package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductSpecificationsCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductSpecificationsUseCase {
    /**
     * Update product specifications (JSONB attributes)
     */
    Product updateSpecifications(UpdateProductSpecificationsCommand command);
}

