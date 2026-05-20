package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductUseCase {
    /**
     * Update product information
     */
    Product updateProduct(UpdateProductCommand command);
}
