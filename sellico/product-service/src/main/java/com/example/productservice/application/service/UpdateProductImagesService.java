package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductImagesCommand;
import com.example.productservice.application.port.in.UpdateProductImagesUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateProductImagesService implements UpdateProductImagesUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public Product updateImages(UpdateProductImagesCommand command) {
        // 1. Find existing product
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. Check ownership
        if (!product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // 3. Validate images
        if (command.getImages() == null || command.getImages().isEmpty()) {
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        // 4. Validate only one primary image
        long primaryCount = command.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .count();
        if (primaryCount > 1) {
            throw new ProductBusinessException(ProductErrorCode.ONLY_ONE_PRIMARY_IMAGE);
        }

        // 5. Replace all images
        List<ProductImage> newImages = command.getImages().stream()
                .map(img -> ProductImage.create(
                        img.getUrl(),
                        img.getIsPrimary(),
                        img.getSortOrder()
                ))
                .collect(Collectors.toList());

        // If no primary image specified, set first one as primary
        if (primaryCount == 0 && !newImages.isEmpty()) {
            newImages.get(0).setIsPrimary(true);
        }

        product.setImages(newImages);

        // 6. Update timestamp
        product.setUpdatedAt(Instant.now());

        // 7. Save and return
        return productRepositoryPort.save(product);
    }
}

