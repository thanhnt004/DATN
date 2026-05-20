package com.example.productservice.adapter.in.web.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch fetching products
 * Endpoint: GET /internal/v1/products/batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProductsRequest {

    @NotEmpty(message = "Product IDs list cannot be empty")
    private List<UUID> productIds;
}

