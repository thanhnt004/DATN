package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.ReserveStockRequest;
import com.example.inventoryservice.dto.response.ReservationResponse;
import com.example.inventoryservice.dto.response.ReserveStockResponse;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Internal controller for reservation management - no authentication required.
 * Used for service-to-service communication (e.g., order-service saga).
 * Base path: /internal/v1/reservations
 */
@RestController
@RequestMapping("/internal/v1/reservations")
@RequiredArgsConstructor
public class InternalReservationController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReserveStockResponse>> reserveStock(
            @Valid @RequestBody ReserveStockRequest request
    ) {
        ReserveStockResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> confirmReservation(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReservationResponse> response = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> cancelReservation(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReservationResponse> response = inventoryService.cancelReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
