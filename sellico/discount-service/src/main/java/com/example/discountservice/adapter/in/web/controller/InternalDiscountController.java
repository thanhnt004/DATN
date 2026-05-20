package com.example.discountservice.adapter.in.web.controller;

import com.example.discountservice.adapter.in.web.request.ApplyCouponRequest;
import com.example.discountservice.adapter.in.web.request.ReleaseCouponRequest;
import com.example.discountservice.adapter.in.web.request.ValidateCouponRequest;
import com.example.discountservice.application.port.in.CouponUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

/**
 * Internal API for service-to-service communication (order-service, etc.)
 * Base path: /internal/v1/discounts
 */
@RestController
@RequestMapping("/internal/v1/discounts")
@RequiredArgsConstructor
public class InternalDiscountController {

    private final CouponUseCase couponUseCase;

    /**
     * POST /internal/v1/discounts/validate — Validate coupon for order
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponUseCase.ValidateCouponResult>> validate(
            @Valid @RequestBody ValidateCouponRequest req) {
        CouponUseCase.ValidateCouponResult result = couponUseCase.validateCoupon(
                req.getCouponId(), req.getUserId(), req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    @PostMapping("/validate/seller")
    public ResponseEntity<ApiResponse<CouponUseCase.ValidateCouponResult>> validateForSeller(
            @Valid @RequestBody ValidateCouponRequest req) {
        CouponUseCase.ValidateCouponResult result = couponUseCase.validateSellerCoupon(
                req.getCouponId(), req.getUserId(),req.getSellerId(), req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * POST /internal/v1/discounts/apply — Apply coupon to confirmed order
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponUseCase.ApplyCouponResult>> apply(
            @Valid @RequestBody ApplyCouponRequest req) {
        CouponUseCase.ApplyCouponResult result = couponUseCase.applyCoupon(
                req.getCouponId(), null, req.getOrderId(), req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * POST /internal/v1/discounts/release — Release coupon (order cancelled/expired)
     */
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<Void>> release(@Valid @RequestBody ReleaseCouponRequest req) {
        couponUseCase.releaseCoupon(req.getCouponId(), req.getUserId(), req.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

