package com.example.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private UUID skuId;
    private Integer totalStock;
    private Integer reservedStock;
    private Integer availableStock;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private String locationCode;
    private Instant updatedAt;
}

