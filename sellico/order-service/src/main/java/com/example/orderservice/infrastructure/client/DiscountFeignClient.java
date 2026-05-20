package com.example.orderservice.infrastructure.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "discount-service")
public interface DiscountFeignClient {
    @PostMapping("/internal/v1/discounts/validate")
    ApiResponse<ValidateCouponResult> validateCoupon(@Valid @RequestBody ValidateCouponRequest req);
    @PostMapping("/internal/v1/discounts/validate/seller")
    ApiResponse<ValidateCouponResult> validateCouponForSeller(@Valid @RequestBody ValidateCouponRequest req);
    record ValidateCouponResult(boolean valid, String message, BigDecimal discountAmount,
                                String couponCode, String discountType) {}
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ValidateCouponRequest {
        @NotNull
        private UUID couponId;
        @NotNull private UUID userId;
        @NotNull private BigDecimal orderAmount;
        private UUID sellerId;
    }
}
