package com.example.cartservice.controller;

import com.example.cartservice.dto.response.CartItemResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for internal service communication
 * Base path: /internal/v1
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;

    /**
     * GET /internal/v1/cart/{userId} - Get cart for user
     */
    @GetMapping("/cart/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable("userId") UUID userId) {
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/cart/{userId}/sku-ids - Get SKU IDs in cart
     */
    @GetMapping("/cart/{userId}/sku-ids")
    public ResponseEntity<ApiResponse<List<UUID>>> getCartSkuIds(@PathVariable("userId") UUID userId) {
        List<UUID> skuIds = cartService.getCartSkuIds(userId);
        return ResponseEntity.ok(ApiResponse.success(skuIds));
    }

    /**
     * GET /internal/v1/cart/{userId}/count - Get cart item count
     */
    @GetMapping("/cart/{userId}/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(@PathVariable("userId") UUID userId) {
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * GET /internal/v1/cart/{userId}/selected-items - Get selected cart items (for order-service checkout)
     */
    @GetMapping("/cart/{userId}/selected-items")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getSelectedItems(@PathVariable("userId") UUID userId) {
        List<CartItemResponse> items = cartService.getSelectedItems(userId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /**
     * DELETE /internal/v1/cart/{userId}/selected - Remove selected items (after checkout)
     */
    @DeleteMapping("/cart/{userId}/selected")
    public ResponseEntity<ApiResponse<Void>> removeSelectedItems(@PathVariable("userId") UUID userId) {
        cartService.removeSelectedItems(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * DELETE /internal/v1/cart/{userId} - Clear cart
     */
    @DeleteMapping("/cart/{userId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable("userId") UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

