package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductImagesCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductImagesUseCase {
    /**
     * Update product images (replace all images)
     */
    Product updateImages(UpdateProductImagesCommand command);
}

