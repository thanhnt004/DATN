package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating product ratings
 * Endpoint: PATCH /internal/v1/products/{id}/ratings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRatingRequest {

    @NotNull(message = "Rating average is required")
    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5")
    private BigDecimal ratingAvg;

    @NotNull(message = "Rating count is required")
    @Min(value = 0, message = "Rating count must be non-negative")
    private Integer ratingCount;
}

