package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import model.SpecAttribute;

@Getter
@AllArgsConstructor
@Value
public class CreateProductCommand {

    String name;
    UUID userId;
    UUID sellerId;
    UUID categoryId;
    String description;

    List<ImageCommand> images;
    List<OptionCommand> options;
    List<SkuCommand> skus;

    List<SpecAttribute> specifications;

    @Getter
    @AllArgsConstructor
    public static class ImageCommand {
        private final String url;
        private final boolean primary;
        private final int sortOrder;
    }

    @Getter
    @AllArgsConstructor
    public static class OptionCommand {
        private final String name;
        private final List<OptionValueCommand> values;
    }

    @Getter
    @AllArgsConstructor
    public static class OptionValueCommand {
        private final String value;
        private final String imageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class SkuCommand {
        private final String skuCode;
        private final BigDecimal price;
        private final BigDecimal originalPrice;
        private final BigDecimal costPrice;
        private final int weightGram;
        private final Integer lengthCm;
        private final Integer widthCm;
        private final Integer heightCm;
        private final Map<String, String> selectionAttributes;

        // Inventory fields
        private final Integer totalStock;
        private final Integer lowStockThreshold;
        private final String locationCode;
    }
}
