package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductRatingCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductRatingUseCase {
    /**
     * Update product rating (for internal service communication - called by review service)
     */
    Product updateRating(UpdateProductRatingCommand command);
}

