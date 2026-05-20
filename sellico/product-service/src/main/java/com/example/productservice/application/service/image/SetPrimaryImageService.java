package com.example.productservice.application.service.image;

import com.example.productservice.application.port.in.image.SetPrimaryImageUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetPrimaryImageService implements SetPrimaryImageUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public void setPrimaryImage(UUID productId, UUID imageId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImage> images = product.getImages();
        if (images == null || images.isEmpty()) {
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        // Verify the image exists before setting as primary
        boolean imageExists = images.stream()
                .anyMatch(img -> img.getId().equals(imageId));

        if (!imageExists) {
            throw new ProductBusinessException(ProductErrorCode.IMAGE_NOT_FOUND);
        }

        // Set all images to non-primary, then set the target as primary
        product.setPrimaryImage(imageId);

        productRepositoryPort.save(product);
    }
}

