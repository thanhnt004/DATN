package com.example.productservice.application.service;

import com.example.productservice.application.port.in.GetRelatedProductsUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRelatedProductsService implements GetRelatedProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Product> getRelatedProducts(UUID productId, int limit) {
        Product product = productRepositoryPort.findById(productId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        int effectiveLimit = (limit > 0 && limit <= 50) ? limit : 12;

        return productRepositoryPort.findByCategoryIdAndStatus(
                product.getCategoryId(), "ACTIVE", effectiveLimit, productId
        );
    }
}

