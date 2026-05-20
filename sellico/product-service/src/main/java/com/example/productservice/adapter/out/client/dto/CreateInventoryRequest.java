package com.example.productservice.adapter.out.client.dto;

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
    private UUID skuId;
    private Integer totalStock;
    @Builder.Default
    private Integer lowStockThreshold = 5;
    private String locationCode;
}
