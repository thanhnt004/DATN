package com.example.productservice.adapter.out.client;

import com.example.productservice.adapter.out.client.dto.CreateInventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.util.List;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/internal/v1/inventories/batch-create")
    ApiResponse<List<Object>> batchCreateInventory(@RequestBody List<CreateInventoryRequest> requests);
}
