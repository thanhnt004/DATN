package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateSkuCommand;
import com.example.productservice.application.port.in.UpdateSkuUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.application.port.out.SkuRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateSkuService implements UpdateSkuUseCase {

    private static final Set<String> VALID_SKU_STATUSES = Set.of("ACTIVE", "DISABLED", "OUT_OF_STOCK");

    private final SkuRepositoryPort skuRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public ProductSku updateSku(UpdateSkuCommand command) {
        // 1. Find existing SKU
        ProductSku sku = skuRepositoryPort.findById(command.getSkuId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.SKU_NOT_FOUND));

        // 2. Check ownership via product
        Product product = skuRepositoryPort.findProductBySkuId(command.getSkuId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // 3. Update fields if provided
        if (command.getPrice() != null) {
            if (command.getPrice().signum() <= 0) {
                throw new ProductBusinessException(ProductErrorCode.PRICE_INVALID);
            }
            sku.setPrice(command.getPrice());
        }

        if (command.getOriginalPrice() != null) {
            if (sku.getPrice() != null && command.getOriginalPrice().compareTo(sku.getPrice()) < 0) {
                throw new ProductBusinessException(ProductErrorCode.ORIGINAL_PRICE_INVALID);
            }
            sku.setOriginalPrice(command.getOriginalPrice());
        }

        if (command.getCostPrice() != null) {
            sku.setCostPrice(command.getCostPrice());
        }

        if (command.getWeightGram() != null) {
            if (command.getWeightGram() <= 0) {
                throw new ProductBusinessException(ProductErrorCode.WEIGHT_INVALID);
            }
            sku.setWeightGram(command.getWeightGram());
        }

        if (command.getLengthCm() != null) {
            sku.setLengthCm(command.getLengthCm());
        }
        if (command.getWidthCm() != null) {
            sku.setWidthCm(command.getWidthCm());
        }
        if (command.getHeightCm() != null) {
            sku.setHeightCm(command.getHeightCm());
        }

        if (command.getStatus() != null) {
            if (!VALID_SKU_STATUSES.contains(command.getStatus())) {
                throw new ProductBusinessException(ProductErrorCode.INVALID_STATUS);
            }
            sku.setStatus(command.getStatus());
        }

        // 4. Save SKU
        ProductSku updatedSku = skuRepositoryPort.save(sku);

        // 5. Recalculate product price range
        product.recalcPriceRange();
        product.setUpdatedAt(Instant.now());
        productRepositoryPort.updateProductOnly(product);

        return updatedSku;
    }
}

