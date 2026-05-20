package com.example.inventoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryRequest {

    @NotNull(message = "SKU ID is required")
    private UUID skuId;

    @Min(value = 0, message = "Total stock must be non-negative")
    @Builder.Default
    private Integer totalStock = 0;

    @Min(value = 0, message = "Low stock threshold must be non-negative")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    private String locationCode;
}

