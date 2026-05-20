package com.example.productservice.application.port.out;

import java.util.List;
import java.util.UUID;

public interface InventoryClientPort {
    /**
     * Create inventory records for a batch of SKUs.
     * Each entry maps skuId -> initial stock quantity.
     */
    void createInventoryForSkus(List<SkuInventoryInfo> skuInventoryInfos);

    record SkuInventoryInfo(UUID skuId, int totalStock, int lowStockThreshold, String locationCode) {}
}
