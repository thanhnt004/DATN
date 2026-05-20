package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import model.SpecAttribute;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateProductCommand {
    UUID productId;
    UUID sellerId;  // For authorization check
    UUID categoryId;
    String name;
    String description;
    List<SpecAttribute> specifications;
    List<ImageCommand> images;
    List<OptionCommand> options;
    List<SkuCommand> skus;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ImageCommand {
        private final String url;
        private final Boolean isPrimary;
        private final Integer sortOrder;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OptionCommand {
        private final String name;
        private final List<OptionValueCommand> values;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OptionValueCommand {
        private final String value;
        private final String imageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuCommand {
        private final String skuCode;
        private final BigDecimal price;
        private final BigDecimal originalPrice;
        private final BigDecimal costPrice;
        private final Integer weightGram;
        private final Integer lengthCm;
        private final Integer widthCm;
        private final Integer heightCm;
        private final List<SkuAttributeCommand> attributes;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuAttributeCommand {
        private final String optionName;
        private final String value;
    }
}
