package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductSkusCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductSkusUseCase {
    /**
     * Update product SKUs (batch update/replace)
     */
    Product updateSkus(UpdateProductSkusCommand command);
}

