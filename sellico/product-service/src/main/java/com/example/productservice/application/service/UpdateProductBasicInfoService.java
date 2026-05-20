package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductBasicInfoCommand;
import com.example.productservice.application.port.in.UpdateProductBasicInfoUseCase;
import com.example.productservice.application.port.out.CategoryClientPort;
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
public class UpdateProductBasicInfoService implements UpdateProductBasicInfoUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final CategoryClientPort categoryClientPort;

    @Override
    @Transactional
    public Product updateBasicInfo(UpdateProductBasicInfoCommand command) {
        // 1. Find existing product
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. Check ownership
        if (!product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // 3. Validate category if changed
        if (command.getCategoryId() != null && !command.getCategoryId().equals(product.getCategoryId())) {
            validateCategory(command.getCategoryId());
            product.setCategoryId(command.getCategoryId());
        }

        // 4. Update basic info
        if (command.getName() != null) {
            product.setName(command.getName());
            // Auto-generate slug if not provided
            if (command.getSlug() == null || command.getSlug().isBlank()) {
                product.setSlug(slugify(command.getName()));
            }
        }

        if (command.getSlug() != null && !command.getSlug().isBlank()) {
            product.setSlug(command.getSlug());
        }

        if (command.getDescription() != null) {
            product.setDescription(command.getDescription());
        }

        // 5. Update timestamp
        product.setUpdatedAt(Instant.now());

        // 6. Save and return
        return productRepositoryPort.updateProductOnly(product);
    }

    private void validateCategory(java.util.UUID categoryId) {
        if (!categoryClientPort.isExist(categoryId)) {
            throw new ProductBusinessException(ProductErrorCode.CATEGORY_NOT_EXISTED);
        }
        if (!categoryClientPort.isLeaf(categoryId)) {
            throw new ProductBusinessException(ProductErrorCode.CATEGORY_IS_NOT_LEAF);
        }
    }

    private String slugify(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}

