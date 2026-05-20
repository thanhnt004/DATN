package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductRatingCommand;
import com.example.productservice.application.port.in.UpdateProductRatingUseCase;
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
public class UpdateProductRatingService implements UpdateProductRatingUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public Product updateRating(UpdateProductRatingCommand command) {
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // Update rating
        product.setRatingAvg(command.getRatingAvg());
        product.setRatingCount(command.getRatingCount());
        product.setUpdatedAt(Instant.now());

        return productRepositoryPort.updateProductOnly(product);
    }
}

