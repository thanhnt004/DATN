package com.example.productservice.adapter.in.web.mapper;

import com.example.productservice.adapter.in.web.request.CreateProductRequest;
import com.example.productservice.adapter.in.web.response.ProductResponse;
import com.example.productservice.application.command.CreateProductCommand;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductImage;
import com.example.productservice.domain.model.ProductOption;
import com.example.productservice.domain.model.ProductOptionValue;
import com.example.productservice.domain.model.ProductSku;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductWebMapper {

    public CreateProductCommand toCommand(CreateProductRequest request, UUID userId) {
        return new CreateProductCommand(
                request.getName(),
                userId,
                request.getSellerId(),
                request.getCategoryId(),
                request.getDescription(),
                request.getImages() != null ? request.getImages().stream()
                        .map(img -> new CreateProductCommand.ImageCommand(
                                img.getUrl(),
                                img.getIsPrimary() != null && img.getIsPrimary(),
                                img.getSortOrder() != null ? img.getSortOrder() : 0
                        ))
                        .collect(Collectors.toList()) : List.of(),
                request.getOptions() != null ? request.getOptions().stream()
                        .map(opt -> new CreateProductCommand.OptionCommand(opt.getName(),
                                opt.getValues().stream()
                                        .map(v -> new CreateProductCommand.OptionValueCommand(v.getValue(), v.getImageUrl()))
                                        .collect(Collectors.toList())))
                        .collect(Collectors.toList()) : List.of(),
                request.getSkus() != null ? request.getSkus().stream()
                        .map(sku -> new CreateProductCommand.SkuCommand(
                                sku.getSkuCode(),
                                sku.getPrice(),
                                sku.getOriginalPrice(),
                                sku.getCostPrice(),
                                sku.getWeightGram() != null ? sku.getWeightGram() : 0,
                                sku.getLengthCm(),
                                sku.getWidthCm(),
                                sku.getHeightCm(),
                                sku.getSelectionAttributes(),
                                sku.getTotalStock(),
                                sku.getLowStockThreshold(),
                                sku.getLocationCode()
                        ))
                        .collect(Collectors.toList()) : List.of(),
                request.getSpecifications()
        );
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .status(product.getStatus())
                .ratingAvg(product.getRatingAvg())
                .ratingCount(product.getRatingCount())
                .soldCount(product.getSoldCount())
                .minPrice(product.getMinPrice())
                .maxPrice(product.getMaxPrice())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .isDeleted(product.getIsDeleted())
                .specifications(product.getSpecifications())
                .images(mapImages(product.getImages()))
                .options(mapOptions(product.getOptions()))
                .skus(mapSkus(product.getSkus(), product.getOptions()))
                .build();
    }

    private List<ProductResponse.ImageResponse> mapImages(List<ProductImage> images) {
        if (images == null) return Collections.emptyList();
        return images.stream()
                .map(img -> ProductResponse.ImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isPrimary(img.getIsPrimary())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();
    }

    private List<ProductResponse.OptionResponse> mapOptions(List<ProductOption> options) {
        if (options == null) return Collections.emptyList();
        return options.stream()
                .map(opt -> ProductResponse.OptionResponse.builder()
                        .id(opt.getId())
                        .name(opt.getName())
                        .source(opt.getSource())
                        .values(opt.getValues() != null ? opt.getValues().stream()
                                .map(v -> ProductResponse.OptionValueResponse.builder()
                                        .id(v.getId())
                                        .value(v.getValue())
                                        .imageUrl(v.getImageUrl())
                                        .build())
                                .toList() : Collections.emptyList())
                        .build())
                .toList();
    }

    private List<ProductResponse.SkuResponse> mapSkus(List<ProductSku> skus, List<ProductOption> options) {
        if (skus == null) return Collections.emptyList();

        // Build lookup: optionValueId → optionName
        Map<UUID, String> valueIdToOptionName = new HashMap<>();
        if (options != null) {
            for (ProductOption opt : options) {
                if (opt.getValues() != null) {
                    for (ProductOptionValue v : opt.getValues()) {
                        valueIdToOptionName.put(v.getId(), opt.getName());
                    }
                }
            }
        }

        return skus.stream()
                .map(sku -> ProductResponse.SkuResponse.builder()
                        .id(sku.getId())
                        .skuCode(sku.getSkuCode())
                        .price(sku.getPrice())
                        .originalPrice(sku.getOriginalPrice())
                        .costPrice(sku.getCostPrice())
                        .status(sku.getStatus())
                        .weightGram(sku.getWeightGram())
                        .lengthCm(sku.getLengthCm())
                        .widthCm(sku.getWidthCm())
                        .heightCm(sku.getHeightCm())
                        .attributes(sku.getSelectedValues() != null ? sku.getSelectedValues().stream()
                                .map(v -> ProductResponse.SkuAttributeResponse.builder()
                                        .optionValueId(v.getId())
                                        .optionName(valueIdToOptionName.getOrDefault(v.getId(), v.getOptionName()))
                                        .valueName(v.getValue())
                                        .build())
                                .toList() : Collections.emptyList())
                        .build())
                .toList();
    }
}
