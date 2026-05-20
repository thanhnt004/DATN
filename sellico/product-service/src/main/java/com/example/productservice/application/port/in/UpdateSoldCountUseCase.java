package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateSoldCountCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateSoldCountUseCase {
    /**
     * Increment sold count for a product (called by order service after successful payment)
     */
    Product updateSoldCount(UpdateSoldCountCommand command);
}

