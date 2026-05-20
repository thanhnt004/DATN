package com.example.inventoryservice.dto.response;

import com.example.inventoryservice.entity.enums.InventoryLogType;
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
public class InventoryLogResponse {
    private UUID id;
    private UUID skuId;
    private Integer changeAmount;
    private InventoryLogType type;
    private UUID referenceId;
    private String note;
    private Instant createdAt;
}

