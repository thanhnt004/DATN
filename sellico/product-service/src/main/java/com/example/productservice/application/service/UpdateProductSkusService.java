package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductSkusCommand;
import com.example.productservice.application.port.in.UpdateProductSkusUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductOptionValue;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateProductSkusService implements UpdateProductSkusUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public Product updateSkus(UpdateProductSkusCommand command) {
        // 1. Find existing product
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. Check ownership
        if (!product.getSellerId().equals(command.getSellerId())) {
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_INVALID);
        }

        // 3. Validate SKUs
        if (command.getSkus() == null || command.getSkus().isEmpty()) {
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_MUST_HAVE_SKU);
        }

        // 4. Check for duplicate SKU codes
        Set<String> skuCodes = new HashSet<>();
        for (var sku : command.getSkus()) {
            if (!skuCodes.add(sku.getSkuCode())) {
                throw new ProductBusinessException(ProductErrorCode.DUPLICATE_SKU_CODE);
            }
        }

        // 5. Validate prices
        for (var sku : command.getSkus()) {
            if (sku.getPrice() == null || sku.getPrice().signum() <= 0) {
                throw new ProductBusinessException(ProductErrorCode.PRICE_INVALID);
            }
            if (sku.getOriginalPrice() != null && sku.getOriginalPrice().compareTo(sku.getPrice()) < 0) {
                throw new ProductBusinessException(ProductErrorCode.ORIGINAL_PRICE_INVALID);
            }
        }

        // 6. Replace all SKUs — preserve existing IDs by matching skuCode
        Map<String, UUID> existingSkuIds = product.getSkus() != null
                ? product.getSkus().stream()
                    .filter(s -> s.getSkuCode() != null && s.getId() != null)
                    .collect(Collectors.toMap(ProductSku::getSkuCode, ProductSku::getId, (a, b) -> a))
                : Collections.emptyMap();

        // Build lookup: "optionName:value" → domain ProductOptionValue (same references as in product.options)
        Map<String, ProductOptionValue> optionValueLookup = product.getOptions() != null
                ? product.getOptions().stream()
                    .flatMap(o -> o.getValues().stream()
                            .map(v -> Map.entry(o.getName() + ":" + v.getValue(), v)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a))
                : Collections.emptyMap();

        List<ProductSku> newSkus = command.getSkus().stream()
                .map(sku -> {
                    UUID skuId = existingSkuIds.getOrDefault(sku.getSkuCode(), UUID.randomUUID());

                    // Resolve selectedValues from selectionAttributes
                    List<ProductOptionValue> selectedValues = sku.getSelectionAttributes() != null
                            ? sku.getSelectionAttributes().entrySet().stream()
                                .map(e -> optionValueLookup.get(e.getKey() + ":" + e.getValue()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                            : Collections.emptyList();

                    return ProductSku.builder()
                            .id(skuId)
                            .skuCode(sku.getSkuCode())
                            .price(sku.getPrice())
                            .originalPrice(sku.getOriginalPrice())
                            .costPrice(sku.getCostPrice())
                            .weightGram(sku.getWeightGram())
                            .lengthCm(sku.getLengthCm())
                            .widthCm(sku.getWidthCm())
                            .heightCm(sku.getHeightCm())
                            .selectedValues(selectedValues)
                            .status("ACTIVE")
                            .build();
                })
                .collect(Collectors.toList());

        product.setSkus(newSkus);

        // 7. Recalculate price range
        product.recalcPriceRange();

        // 8. Update timestamp
        product.setUpdatedAt(Instant.now());

        // 9. Save and return
        Product saved = productRepositoryPort.save(product);

        return saved;
    }
}


