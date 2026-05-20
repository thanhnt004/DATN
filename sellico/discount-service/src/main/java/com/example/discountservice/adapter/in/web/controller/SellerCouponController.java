package com.example.discountservice.adapter.in.web.controller;

import com.example.discountservice.adapter.in.web.request.CreateCouponRequest;
import com.example.discountservice.adapter.in.web.request.UpdateCouponRequest;
import com.example.discountservice.adapter.in.web.response.CouponResponse;
import com.example.discountservice.adapter.out.client.SellerClient;
import com.example.discountservice.application.port.in.CouponUseCase;
import com.example.discountservice.domain.model.Coupon;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.net.URI;
import java.util.UUID;

/**
 * Seller coupon management (shop vouchers)
 * Base path: /api/v1/seller/coupons
 */
@RestController
@RequestMapping("/api/v1/seller/coupons")
@RequiredArgsConstructor
public class SellerCouponController {

    private final CouponUseCase couponUseCase;
    private final SellerClient sellerClient;

    private UUID resolveSellerId(Jwt jwt) {
        var resp = sellerClient.getSellerByUserId(UUID.fromString(jwt.getSubject()));
        if (resp != null && resp.getData() != null) {
            return resp.getData().getId();
        }
        throw new RuntimeException("Seller not found for userId: " + jwt.getSubject());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCouponRequest req) {
        UUID sellerId = resolveSellerId(jwt);
        var rules = req.getRules() != null
                ? req.getRules().stream().map(r -> new CouponUseCase.RuleItem(r.getRuleType(), r.getTargetId())).toList()
                : null;
        Coupon coupon = couponUseCase.createCoupon(new CouponUseCase.CreateCouponCommand(
                req.getCampaignId(), sellerId, req.getCode(), "SHOP",
                req.getDiscountType(), req.getDiscountValue(), req.getMinOrderAmount(),
                req.getMaxDiscountAmount(), req.getTotalQuantity(), req.getMaxUsagePerUser(),
                req.isStackable(), req.getStartDate(), req.getEndDate(), rules));
        return ResponseEntity.created(URI.create("/api/v1/seller/coupons/" + coupon.getId()))
                .body(ApiResponse.success(CouponResponse.from(coupon)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> listMyCoupons(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID sellerId = resolveSellerId(jwt);
        return ResponseEntity.ok(ApiResponse.success(
                couponUseCase.listCoupons(sellerId, status, page, size).map(CouponResponse::from)));
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

