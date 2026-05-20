package com.example.cartservice.controller;

import com.example.cartservice.dto.request.*;
import com.example.cartservice.dto.response.*;
import com.example.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Controller for cart operations
 * Base path: /api/v1/cart
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // =====================================================
    // Cart Operations
    // =====================================================

    /**
     * GET /api/v1/cart - Get current user's cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/cart/by-seller - Get cart items grouped by seller
     */
    @GetMapping("/by-seller")
    public ResponseEntity<ApiResponse<List<CartBySellerResponse>>> getCartGroupedBySeller(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<CartBySellerResponse> response = cartService.getCartGroupedBySeller(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/cart/count - Get cart item count (for badge)
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * POST /api/v1/cart/items - Add item to cart
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/cart/items/{skuId} - Update cart item quantity
     */
    @PutMapping("/items/{skuId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable("skuId") UUID skuId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.updateCartItem(userId, skuId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/cart/items/{skuId} - Remove item from cart
     */
    @DeleteMapping("/items/{skuId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable("skuId") UUID skuId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.removeFromCart(userId, skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/cart/items - Remove multiple items from cart
     */
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> removeMultipleItems(
            @Valid @RequestBody RemoveItemsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.removeMultipleItems(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/cart - Clear entire cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Selection Operations
    // =====================================================

    /**
     * PATCH /api/v1/cart/items/{skuId}/select - Select/deselect item
     */
    @PatchMapping("/items/{skuId}/select")
    public ResponseEntity<ApiResponse<CartResponse>> selectItem(
            @PathVariable("skuId") UUID skuId,
            @RequestParam("selected") boolean selected,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.selectItem(userId, skuId, selected);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PATCH /api/v1/cart/selection - Update selection for multiple items
     */
    @PatchMapping("/selection")
    public ResponseEntity<ApiResponse<CartResponse>> updateSelection(
            @Valid @RequestBody UpdateSelectionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.updateSelection(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/cart/select-all - Select all items
     */
    @PostMapping("/select-all")
    public ResponseEntity<ApiResponse<CartResponse>> selectAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UpdateSelectionRequest request = UpdateSelectionRequest.builder().selected(true).build();
        CartResponse response = cartService.updateSelection(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/cart/deselect-all - Deselect all items
     */
    @PostMapping("/deselect-all")
    public ResponseEntity<ApiResponse<CartResponse>> deselectAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UpdateSelectionRequest request = UpdateSelectionRequest.builder().selected(false).build();
        CartResponse response = cartService.updateSelection(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Checkout Preview
    // =====================================================

    /**
     * GET /api/v1/cart/checkout-preview - Get selected items for checkout
     */
    @GetMapping("/checkout-preview")
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCheckoutPreview(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<CartItemResponse> response = cartService.getSelectedItems(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/cart/checkout-preview/by-seller - Get selected items grouped by seller
     */
    @GetMapping("/checkout-preview/by-seller")
    public ResponseEntity<ApiResponse<List<CartBySellerResponse>>> getCheckoutPreviewBySeller(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<CartBySellerResponse> response = cartService.getSelectedItemsGroupedBySeller(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

