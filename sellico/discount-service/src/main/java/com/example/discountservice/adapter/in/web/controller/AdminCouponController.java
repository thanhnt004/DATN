package com.example.discountservice.adapter.in.web.controller;

import com.example.discountservice.adapter.in.web.request.CreateCouponRequest;
import com.example.discountservice.adapter.in.web.request.UpdateCouponRequest;
import com.example.discountservice.adapter.in.web.response.CouponResponse;
import com.example.discountservice.application.port.in.CouponUseCase;
import com.example.discountservice.domain.model.Coupon;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.net.URI;
import java.util.UUID;

/**
 * Admin coupon management (platform-wide)
 * Base path: /api/v1/admin/coupons
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponUseCase couponUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CreateCouponRequest req) {
        var rules = req.getRules() != null
                ? req.getRules().stream().map(r -> new CouponUseCase.RuleItem(r.getRuleType(), r.getTargetId())).toList()
                : null;
        // Admin always creates PLATFORM coupons
        Coupon coupon = couponUseCase.createCoupon(new CouponUseCase.CreateCouponCommand(
                req.getCampaignId(), null, req.getCode(), "PLATFORM",
                req.getDiscountType(), req.getDiscountValue(), req.getMinOrderAmount(),
                req.getMaxDiscountAmount(), req.getTotalQuantity(), req.getMaxUsagePerUser(),
                req.isStackable(), req.getStartDate(), req.getEndDate(), rules));
        return ResponseEntity.created(URI.create("/api/v1/admin/coupons/" + coupon.getId()))
                .body(ApiResponse.success(CouponResponse.from(coupon)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // Admin only manages platform coupons (not shop coupons)
        return ResponseEntity.ok(ApiResponse.success(
                couponUseCase.listCouponsByType("PLATFORM", status, page, size).map(CouponResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(CouponResponse.from(couponUseCase.getCoupon(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> update(@PathVariable("id") UUID id,
                                                                @RequestBody UpdateCouponRequest req) {
        Coupon updated = couponUseCase.updateCoupon(id, new CouponUseCase.UpdateCouponCommand(
                req.getCode(), req.getDiscountType(), req.getDiscountValue(),
                req.getMinOrderAmount(), req.getMaxDiscountAmount(),
                req.getTotalQuantity(), req.getMaxUsagePerUser(), req.getIsStackable(),
                req.getStatus(), req.getStartDate(), req.getEndDate()));
        return ResponseEntity.ok(ApiResponse.success(CouponResponse.from(updated)));
    }
}

