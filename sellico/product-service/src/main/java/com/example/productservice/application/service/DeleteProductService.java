package com.example.productservice.application.service;

import com.example.productservice.application.port.in.DeleteProductUseCase;
import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteProductService implements DeleteProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final ProductEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void deleteProduct(UUID productId, UUID sellerId) {
        Product product = productRepositoryPort.findById(productId)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // Check if the seller owns this product
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // Soft delete
        product.setIsDeleted(true);
        product.setStatus("DELETED");
        product.setUpdatedAt(Instant.now());

        productRepositoryPort.updateProductOnly(product);
        eventPublisher.publishProductDeleted(productId.toString());
    }
}

