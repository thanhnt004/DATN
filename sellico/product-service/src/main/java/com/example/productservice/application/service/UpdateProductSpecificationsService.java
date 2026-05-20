package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductSpecificationsCommand;
import com.example.productservice.application.port.in.UpdateProductSpecificationsUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateProductSpecificationsService implements UpdateProductSpecificationsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public Product updateSpecifications(UpdateProductSpecificationsCommand command) {
        // 1. Find existing product
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. Check ownership
        if (!product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // 3. Replace specifications (full JSONB replacement)
        // 4. Update timestamp is handled in repository
        // 5. Save and return
        return productRepositoryPort.updateSpecifications(command.getProductId(), command.getSpecifications());
    }
}

