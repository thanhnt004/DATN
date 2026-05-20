package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.CreateInventoryRequest;
import com.example.inventoryservice.dto.response.InventoryResponse;
import com.example.inventoryservice.dto.response.StockAvailabilityResponse;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for internal service communication endpoints
 * Base path: /internal/v1
 *
 * These endpoints are used for inter-service communication.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    /**
     * GET /internal/v1/inventories/batch - Get inventories by SKU IDs
     * Used by: Order Service, Product Service, etc.
     */
    @GetMapping("/inventories/batch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getBatchInventories(
            @RequestParam("skuIds") List<UUID> skuIds
    ) {
        List<InventoryResponse> response = inventoryService.getInventories(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/inventories/batch - Get inventories by SKU IDs (POST version)
     */
    @PostMapping("/inventories/batch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getBatchInventoriesPost(
            @RequestBody List<UUID> skuIds
    ) {
        List<InventoryResponse> response = inventoryService.getInventories(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/inventories/availability - Check stock availability
     * Used by: Cart Service, Order Service, etc.
     */
    @GetMapping("/inventories/availability")
    public ResponseEntity<ApiResponse<List<StockAvailabilityResponse>>> checkAvailability(
            @RequestParam("skuIds") List<UUID> skuIds
    ) {
        List<StockAvailabilityResponse> response = inventoryService.checkAvailability(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/inventories/availability - Check stock availability (POST version)
     */
    @PostMapping("/inventories/availability")
    public ResponseEntity<ApiResponse<List<StockAvailabilityResponse>>> checkAvailabilityPost(
            @RequestBody List<UUID> skuIds
    ) {
        List<StockAvailabilityResponse> response = inventoryService.checkAvailability(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/inventories/{skuId} - Get single inventory
     */
    @GetMapping("/inventories/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable("skuId") UUID skuId
    ) {
        InventoryResponse response = inventoryService.getInventory(skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/inventories/batch-create - Batch create or update inventories
     * Used by: Product Service after creating a product with SKUs
     */
    @PostMapping("/inventories/batch-create")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> batchCreateInventory(
            @Valid @RequestBody List<CreateInventoryRequest> requests
    ) {
        List<InventoryResponse> response = inventoryService.batchUpsertInventory(requests);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

