package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating sold count
 * Endpoint: PATCH /internal/v1/products/{id}/sold-count
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSoldCountRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}

