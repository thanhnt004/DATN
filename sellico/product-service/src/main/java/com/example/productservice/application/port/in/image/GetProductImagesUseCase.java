package com.example.productservice.application.port.in.image;

import com.example.productservice.domain.model.ProductImage;

import java.util.List;
import java.util.UUID;

public interface GetProductImagesUseCase {
    List<ProductImage> getProductImages(UUID productId);
}
