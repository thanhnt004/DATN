package com.example.cartservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSelectionRequest {

    @NotNull(message = "Selected status is required")
    private Boolean selected;

    // Optional: specific SKU IDs to update. If null, update all items
    private List<UUID> skuIds;

    // Optional: specific seller ID to update. If provided, only update items from this seller
    private UUID sellerId;
}

