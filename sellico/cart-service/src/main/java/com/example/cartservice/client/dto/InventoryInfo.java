package com.example.cartservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryInfo {
    private UUID skuId;
    private Integer totalStock;
    private Integer reservedStock;
    private Integer availableStock;
    private Boolean isAvailable;
    private Boolean isLowStock;
}

