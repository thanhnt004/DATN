package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateProductSkusCommand {
    UUID productId;
    UUID sellerId;
    List<SkuItem> skus;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkuItem {
        private final String skuCode;
        private final BigDecimal price;
        private final BigDecimal originalPrice;
        private final BigDecimal costPrice;
        private final Integer weightGram;
        private final Integer lengthCm;
        private final Integer widthCm;
        private final Integer heightCm;
        private final Map<String, String> selectionAttributes;
    }
}

