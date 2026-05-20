package com.example.productservice.application.service.image;

import com.example.productservice.application.port.in.image.GetProductImagesUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetProductImagesService implements GetProductImagesUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    public List<ProductImage> getProductImages(UUID productId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImage> images = product.getImages();
        if (images == null) {
            return Collections.emptyList();
        }

        // Sort by sortOrder ascending, primary images first
        return images.stream()
                .sorted((a, b) -> {
                    // Primary image comes first
                    if (Boolean.TRUE.equals(a.getIsPrimary()) && !Boolean.TRUE.equals(b.getIsPrimary())) {
                        return -1;
                    }
                    if (!Boolean.TRUE.equals(a.getIsPrimary()) && Boolean.TRUE.equals(b.getIsPrimary())) {
                        return 1;
                    }
                    // Then sort by sortOrder
                    int orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
                    int orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
                    return Integer.compare(orderA, orderB);
                })
                .toList();
    }
}

