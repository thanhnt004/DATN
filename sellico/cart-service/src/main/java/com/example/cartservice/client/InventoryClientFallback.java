package com.example.cartservice.client;

import com.example.cartservice.client.dto.InventoryInfo;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Component
public class InventoryClientFallback implements InventoryClient {

    @Override
    public ApiResponse<InventoryInfo> getInventory(UUID skuId) {
        return null;
    }

    @Override
    public ApiResponse<List<InventoryInfo>> checkAvailability(List<UUID> skuIds) {
        return null;
    }
}

