package com.example.productservice.application.service.image;

import com.example.productservice.application.port.in.image.DeleteImageUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteImageService implements DeleteImageUseCase {
    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // Remove image from product (domain logic handles reordering and primary reassignment)
        product.removeImage(imageId);

        productRepositoryPort.save(product);
    }
}
