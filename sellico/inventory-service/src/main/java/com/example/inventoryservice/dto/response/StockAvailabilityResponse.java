package com.example.inventoryservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailabilityResponse {
    private UUID skuId;
    private Integer availableStock;
    private Boolean isAvailable;
    private Boolean isLowStock;
}

