package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.ConfirmReservationRequest;
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
 * Controller for reservation management endpoints
 * Base path: /api/v1/reservations
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final InventoryService inventoryService;

    /**
     * POST /api/v1/reservations - Create stock reservation
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReserveStockResponse>> reserveStock(
            @Valid @RequestBody ReserveStockRequest request
    ) {
        ReserveStockResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/reservations/{orderId}/confirm - Confirm reservation
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> confirmReservation(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReservationResponse> response = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/reservations/{orderId}/cancel - Cancel reservation
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> cancelReservation(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReservationResponse> response = inventoryService.cancelReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/reservations/order/{orderId} - Get reservations by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getOrderReservations(
            @PathVariable("orderId") UUID orderId
    ) {
        List<ReservationResponse> response = inventoryService.getOrderReservations(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

