package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.*;
import com.example.inventoryservice.dto.response.*;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for inventory management endpoints
 * Base path: /api/v1/inventories
 */
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // =====================================================
    // Inventory CRUD
    // =====================================================

    /**
     * POST /api/v1/inventories - Create inventory for a SKU
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/inventories/batch - Batch create or update inventories
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> batchUpsertInventory(
            @Valid @RequestBody List<CreateInventoryRequest> requests
    ) {
        List<InventoryResponse> response = inventoryService.batchUpsertInventory(requests);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/inventories/check-availability - Check stock availability for multiple SKUs (public)
     */
    @GetMapping("/check-availability")
    public ResponseEntity<ApiResponse<List<StockAvailabilityResponse>>> checkAvailability(
            @RequestParam("skuIds") List<UUID> skuIds
    ) {
        List<StockAvailabilityResponse> response = inventoryService.checkAvailability(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/inventories/batch - Get inventories by SKU IDs
     */
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getBatchInventories(
            @RequestParam("skuIds") List<UUID> skuIds
    ) {
        List<InventoryResponse> response = inventoryService.getInventories(skuIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/inventories/{skuId} - Get inventory by SKU ID
     */
    @GetMapping("/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable("skuId") UUID skuId
    ) {
        InventoryResponse response = inventoryService.getInventory(skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/inventories/{skuId} - Update inventory settings
     */
    @PutMapping("/{skuId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable("skuId") UUID skuId,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        InventoryResponse response = inventoryService.updateInventory(skuId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Stock Operations
    // =====================================================

    /**
     * POST /api/v1/inventories/restock - Restock inventory
     */
    @PostMapping("/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restock(
            @Valid @RequestBody RestockRequest request
    ) {
        InventoryResponse response = inventoryService.restock(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/inventories/adjust - Adjust stock (increase or decrease)
     */
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @Valid @RequestBody AdjustStockRequest request
    ) {
        InventoryResponse response = inventoryService.adjustStock(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/inventories/low-stock - Get low stock inventories
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockInventories() {
        List<InventoryResponse> response = inventoryService.getLowStockInventories();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Logs
    // =====================================================

    /**
     * GET /api/v1/inventories/{skuId}/logs - Get inventory logs
     */
    @GetMapping("/{skuId}/logs")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getInventoryLogs(
            @PathVariable("skuId") UUID skuId
    ) {
        List<InventoryLogResponse> response = inventoryService.getInventoryLogs(skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Reservations
    // =====================================================

    /**
     * GET /api/v1/inventories/{skuId}/reservations - Get reservations for a SKU
     */
    @GetMapping("/{skuId}/reservations")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservations(
            @PathVariable("skuId") UUID skuId
    ) {
        List<ReservationResponse> response = inventoryService.getReservations(skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

