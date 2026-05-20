package com.example.productservice.adapter.out.client;

import com.example.productservice.adapter.out.client.dto.CreateInventoryRequest;
import com.example.productservice.application.port.out.InventoryClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClientAdapter implements InventoryClientPort {

    private final InventoryClient inventoryClient;

    @Override
    public void createInventoryForSkus(List<SkuInventoryInfo> skuInventoryInfos) {
        if (skuInventoryInfos == null || skuInventoryInfos.isEmpty()) {
            return;
        }

        List<CreateInventoryRequest> requests = skuInventoryInfos.stream()
                .map(info -> CreateInventoryRequest.builder()
                        .skuId(info.skuId())
                        .totalStock(info.totalStock())
                        .lowStockThreshold(info.lowStockThreshold())
                        .locationCode(info.locationCode())
                        .build())
                .toList();

        try {
            inventoryClient.batchCreateInventory(requests);
            log.info("Successfully created inventory for {} SKUs", requests.size());
        } catch (Exception e) {
            log.error("Failed to create inventory for SKUs: {}", e.getMessage(), e);
            // Don't throw - product creation should still succeed even if inventory creation fails
            // Inventory can be created later manually
        }
    }
}
