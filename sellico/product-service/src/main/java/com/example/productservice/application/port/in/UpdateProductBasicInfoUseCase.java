package com.example.productservice.application.port.in;

import com.example.productservice.application.command.UpdateProductBasicInfoCommand;
import com.example.productservice.domain.model.Product;

public interface UpdateProductBasicInfoUseCase {
    /**
     * Update basic product information (name, slug, description, categoryId)
     */
    Product updateBasicInfo(UpdateProductBasicInfoCommand command);
}

