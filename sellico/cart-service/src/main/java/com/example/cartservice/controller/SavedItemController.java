package com.example.cartservice.controller;

                 import com.example.cartservice.dto.request.SaveForLaterRequest;
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
 * Controller for saved items (wishlist functionality)
 * Base path: /api/v1/cart/saved
 */
@RestController
@RequestMapping("/api/v1/cart/saved")
@RequiredArgsConstructor
public class SavedItemController {

    private final CartService cartService;

    /**
     * GET /api/v1/cart/saved - Get saved items
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedItemResponse>>> getSavedItems(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<SavedItemResponse> response = cartService.getSavedItems(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/cart/saved/count - Get saved item count (for badge)
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getSavedItemCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        long count = cartService.getSavedItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * POST /api/v1/cart/saved - Save item directly to wishlist (from product page)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SavedItemResponse>> saveItemDirectly(
            @Valid @RequestBody SaveForLaterRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SavedItemResponse response = cartService.saveItemDirectly(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/cart/saved/{skuId}/from-cart - Save item for later (move from cart)
     */
    @PostMapping("/{skuId}/from-cart")
    public ResponseEntity<ApiResponse<SavedItemResponse>> saveForLater(
            @PathVariable("skuId") UUID skuId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SavedItemResponse response = cartService.saveForLater(userId, skuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/cart/saved/{skuId}/move-to-cart - Move saved item to cart
     */
    @PostMapping("/{skuId}/move-to-cart")
    public ResponseEntity<ApiResponse<CartResponse>> moveToCart(
            @PathVariable("skuId") UUID skuId,
            @RequestParam("sellerId") UUID sellerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CartResponse response = cartService.moveToCart(userId, skuId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * DELETE /api/v1/cart/saved/{skuId} - Remove saved item
     */
    @DeleteMapping("/{skuId}")
    public ResponseEntity<ApiResponse<Void>> removeSavedItem(
            @PathVariable("skuId") UUID skuId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        cartService.removeSavedItem(userId, skuId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

