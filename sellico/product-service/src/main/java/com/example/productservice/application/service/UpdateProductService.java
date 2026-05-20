package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateProductCommand;
import com.example.productservice.application.port.in.UpdateProductUseCase;
import com.example.productservice.application.port.out.CategoryClientPort;
import com.example.productservice.application.port.out.ProductEventPublisherPort;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import com.example.productservice.domain.model.ProductOption;
import com.example.productservice.domain.model.ProductOptionValue;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateProductService implements UpdateProductUseCase {

    private final ProductRepositoryPort productRepositoryPort;
    private final CategoryClientPort categoryClientPort;
    private final ProductEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public Product updateProduct(UpdateProductCommand command) {
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
            product.setSlug(slugify(command.getName()));
        }

        if (command.getDescription() != null) {
            product.setDescription(command.getDescription());
        }

        if (command.getSpecifications() != null) {
            product.setSpecifications(command.getSpecifications());
        }

        // 5. Update images if provided — preserve existing IDs by matching URL
        if (command.getImages() != null) {
            Map<String, UUID> existingImageIds = product.getImages() != null
                    ? product.getImages().stream()
                        .filter(img -> img.getUrl() != null && img.getId() != null)
                        .collect(Collectors.toMap(ProductImage::getUrl, ProductImage::getId, (a, b) -> a))
                    : Collections.emptyMap();

            List<ProductImage> newImages = command.getImages().stream()
                    .map(img -> {
                        UUID existingId = existingImageIds.get(img.getUrl());
                        return ProductImage.builder()
                                .id(existingId != null ? existingId : UUID.randomUUID())
                                .url(img.getUrl())
                                .isPrimary(img.getIsPrimary() != null && img.getIsPrimary())
                                .sortOrder(img.getSortOrder() != null ? img.getSortOrder() : 0)
                                .build();
                    })
                    .collect(Collectors.toList());
            product.setImages(newImages);
        }

        // 6. Update options if provided — preserve existing IDs by matching option name + value
        if (command.getOptions() != null) {
            // Build lookup: optionName -> existing option
            Map<String, ProductOption> existingOptionsByName = product.getOptions() != null
                    ? product.getOptions().stream()
                        .filter(o -> o.getName() != null && o.getId() != null)
                        .collect(Collectors.toMap(ProductOption::getName, Function.identity(), (a, b) -> a))
                    : Collections.emptyMap();

            List<ProductOption> newOptions = command.getOptions().stream()
                    .map(opt -> {
                        ProductOption existingOpt = existingOptionsByName.get(opt.getName());
                        UUID optionId = existingOpt != null ? existingOpt.getId() : UUID.randomUUID();

                        // Build lookup for existing option values
                        Map<String, UUID> existingValueIds = existingOpt != null && existingOpt.getValues() != null
                                ? existingOpt.getValues().stream()
                                    .filter(v -> v.getValue() != null && v.getId() != null)
                                    .collect(Collectors.toMap(ProductOptionValue::getValue, ProductOptionValue::getId, (a, b) -> a))
                                : Collections.emptyMap();

                        List<ProductOptionValue> values = opt.getValues().stream()
                                .map(v -> {
                                    UUID valueId = existingValueIds.getOrDefault(v.getValue(), UUID.randomUUID());
                                    return ProductOptionValue.builder()
                                            .id(valueId)
                                            .value(v.getValue())
                                            .imageUrl(v.getImageUrl())
                                            .build();
                                })
                                .collect(Collectors.toList());

                        return ProductOption.builder()
                                .id(optionId)
                                .name(opt.getName())
                                .values(values)
                                .build();
                    })
                    .collect(Collectors.toList());
            product.setOptions(newOptions);
        }

        // 7. Update SKUs if provided — preserve existing IDs by matching skuCode
        if (command.getSkus() != null && !command.getSkus().isEmpty()) {
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

                        // Resolve selectedValues from SKU attributes
                        List<ProductOptionValue> selectedValues = sku.getAttributes() != null
                                ? sku.getAttributes().stream()
                                    .map(attr -> optionValueLookup.get(attr.getOptionName() + ":" + attr.getValue()))
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
                                .selectedValues(selectedValues)
                                .status("ACTIVE")
                                .build();
                    })
                    .collect(Collectors.toList());
            product.setSkus(newSkus);
            product.recalcPriceRange();
        }

        // 8. Update timestamp
        product.setUpdatedAt(Instant.now());

        // 9. Save and return
        Product saved = productRepositoryPort.save(product);
        eventPublisher.publishProductUpdated(saved);
        return saved;
    }

    private void validateCategory(UUID categoryId) {
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

