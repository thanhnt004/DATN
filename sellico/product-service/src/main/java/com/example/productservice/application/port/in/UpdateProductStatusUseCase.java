package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductStatusCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductStatusUseCase {
    /**
     * Update product status (DRAFT, PENDING, ACTIVE, BANNED, DELETED)
     */
    Product updateStatus(UpdateProductStatusCommand command);
}
