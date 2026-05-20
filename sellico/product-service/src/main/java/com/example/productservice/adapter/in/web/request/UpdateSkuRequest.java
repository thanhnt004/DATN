package com.example.productservice.adapter.in.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for updating a single SKU
 * Endpoint: PATCH /v1/seller/skus/{skuId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSkuRequest {

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

    private String status; // ACTIVE, DISABLED

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

