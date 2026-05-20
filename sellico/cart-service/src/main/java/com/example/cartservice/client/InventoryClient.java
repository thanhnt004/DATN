package com.example.cartservice.client;

import com.example.cartservice.client.dto.InventoryInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @GetMapping("/internal/v1/inventories/{skuId}")
    ApiResponse<InventoryInfo> getInventory(@PathVariable("skuId") UUID skuId);

    @GetMapping("/internal/v1/inventories/availability")
    ApiResponse<List<InventoryInfo>> checkAvailability(@RequestParam("skuIds") List<UUID> skuIds);
}

