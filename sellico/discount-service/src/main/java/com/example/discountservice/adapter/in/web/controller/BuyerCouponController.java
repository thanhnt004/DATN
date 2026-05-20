package com.example.discountservice.adapter.in.web.controller;

import com.example.discountservice.adapter.in.web.request.ApplyCouponRequest;
import com.example.discountservice.adapter.in.web.response.CouponResponse;
import com.example.discountservice.adapter.in.web.response.UserCouponResponse;
import com.example.discountservice.application.port.in.CouponUseCase;
import com.example.discountservice.domain.model.Coupon;
import com.example.discountservice.domain.model.UserCouponClaim;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Buyer-facing coupon endpoints
 * Base path: /api/v1/coupons
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class BuyerCouponController {

    private final CouponUseCase couponUseCase;

    /**
     * GET /api/v1/coupons/available — List available coupons (public, optionally by shop)
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> listAvailable(
            @RequestParam(value = "sellerId", required = false) UUID sellerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<CouponResponse> resp = couponUseCase.listAvailableCoupons(sellerId, page, size)
                .map(CouponResponse::from);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * GET /api/v1/coupons/{id} — Get coupon details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(CouponResponse.from(couponUseCase.getCoupon(id))));
    }

    /**
     * POST /api/v1/coupons/{id}/claim — Claim a coupon
     */
    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<UserCouponResponse>> claim(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UserCouponClaim claim = couponUseCase.claimCoupon(id, userId);
        return ResponseEntity.ok(ApiResponse.success(UserCouponResponse.from(claim)));
    }

    /**
     * GET /api/v1/coupons/my — Get my claimed coupons
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UserCouponResponse>>> myCoupons(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<UserCouponResponse> resp = couponUseCase.getUserCoupons(userId, status, page, size)
                .stream().map(UserCouponResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    /**
     * POST /api/v1/coupons/apply — Preview coupon discount on order (before checkout)
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponUseCase.ApplyCouponResult>> applyPreview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplyCouponRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CouponUseCase.ApplyCouponResult result = couponUseCase.applyCouponPreview(
                req.getCouponId(), userId, req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

