package com.example.productservice.application.mapper;

import com.example.productservice.application.command.CreateProductCommand;
import com.example.productservice.domain.model.*;
import model.SpecAttribute;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ProductMapper {

    public static Product toDomain(CreateProductCommand cmd) {
        List<ProductOption> productOptions = mapOptions(cmd.getOptions());
        List<ProductSku> skus = toDomainSkus(cmd, productOptions);

        return Product.createNew(
                cmd.getSellerId(),
                cmd.getCategoryId(),
                cmd.getName(),
                cmd.getDescription(),
                mapSpecifications(cmd.getSpecifications()),
                mapImages(cmd.getImages()),
                productOptions,
                skus
        );
    }

    private static List<ProductImage> mapImages(List<CreateProductCommand.ImageCommand> images) {
        if (images == null) return List.of();

        return images.stream()
                .map(i -> ProductImage.builder()
                        .id(UUID.randomUUID())
                        .url(i.getUrl())
                        .isPrimary(i.isPrimary())
                        .sortOrder(i.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<ProductOption> mapOptions(List<CreateProductCommand.OptionCommand> options) {
        if (options == null) return List.of();

        return options.stream()
                .map(o -> ProductOption.builder()
                        .id(UUID.randomUUID())
                        .name(o.getName())
                        .values(getValues(o.getValues()))
                        .build())
                .collect(Collectors.toList());
    }
    private static List<ProductOptionValue> getValues(List<CreateProductCommand.OptionValueCommand> values) {
        return values.stream().map(v -> ProductOptionValue.builder()
                .value(v.getValue())
                .imageUrl(v.getImageUrl())
                .build()).toList();
    }
    private static List<ProductSku> toDomainSkus(
            CreateProductCommand command,
            List<ProductOption> options
    ) {
        Map<String, ProductOptionValue> optionValueLookup =
                options.stream()
                        .flatMap(o -> o.getValues().stream()
                                .map(v -> Map.entry(o.getName() + ":" + v.getValue(), v))
                        )
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<ProductSku> skus = command.getSkus().stream()
                .map(skuCmd -> {
                    List<ProductOptionValue> selectedValues =
                            skuCmd.getSelectionAttributes().entrySet().stream()
                                    .map(e -> {
                                        String key = e.getKey() + ":" + e.getValue();
                                        ProductOptionValue v = optionValueLookup.get(key);
                                        if (v == null) {
                                            throw new IllegalArgumentException("Invalid option: " + key);
                                        }
                                        return v;
                                    })
                                    .toList();

                    return ProductSku.builder()
                            .skuCode(skuCmd.getSkuCode())
                            .price(skuCmd.getPrice())
                            .originalPrice(skuCmd.getOriginalPrice())
                            .costPrice(skuCmd.getCostPrice())
                            .weightGram(skuCmd.getWeightGram())
                            .lengthCm(skuCmd.getLengthCm())
                            .widthCm(skuCmd.getWidthCm())
                            .heightCm(skuCmd.getHeightCm())
                            .selectedValues(selectedValues)
                            .build();
                })
                .toList();
        return skus;
    }

    private static List<SpecAttribute> mapSpecifications(List<SpecAttribute> specs) {
        if (specs == null) return List.of();
        return new ArrayList<>(specs);
    }

}
