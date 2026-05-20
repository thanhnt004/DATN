package com.example.productservice.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating product SKUs (batch update)
 * Endpoint: PATCH /v1/seller/products/{id}/skus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductSkusRequest {

    @NotEmpty(message = "At least one SKU is required")
    @Valid
    private List<SkuItem> skus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuItem {
        @NotBlank(message = "SKU code is required")
        private String skuCode;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal price;

        @DecimalMin(value = "0.00", message = "Original price must be non-negative")
        private BigDecimal originalPrice;

        @DecimalMin(value = "0.00", message = "Cost price must be non-negative")
        private BigDecimal costPrice;

        @Min(value = 1, message = "Weight must be at least 1 gram")
        private Integer weightGram;

        @Valid
        private DimensionItem dimensions;

        /**
         * Selection attributes mapping option to value
         * Example: {"Color": "Red", "Size": "XL"}
         */
        @NotEmpty(message = "Selection attributes are required")
        private Map<String, String> selectionAttributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionItem {
        @Min(value = 1, message = "Length must be at least 1 cm")
        private Integer lengthCm;

        @Min(value = 1, message = "Width must be at least 1 cm")
        private Integer widthCm;

        @Min(value = 1, message = "Height must be at least 1 cm")
        private Integer heightCm;
    }
}

