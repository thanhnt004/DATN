package com.example.discountservice.application.port.in;

import com.example.discountservice.domain.model.Coupon;
import com.example.discountservice.domain.model.UserCouponClaim;
import com.example.discountservice.domain.model.enums.RuleType;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Consolidated use cases for coupon operations
 */
public interface CouponUseCase {

    // ---- Admin / Seller CRUD ----
    Coupon createCoupon(CreateCouponCommand command);
    Coupon updateCoupon(UUID id, UpdateCouponCommand command);
    Coupon getCoupon(UUID id);
    Page<Coupon> listCoupons(UUID sellerId, String status, int page, int size);
    Page<Coupon> listCouponsByType(String couponType, String status, int page, int size);

    // ---- Buyer ----
    Page<Coupon> listAvailableCoupons(UUID sellerId, int page, int size);
    UserCouponClaim claimCoupon(UUID couponId, UUID userId);
    List<UserCouponClaim> getUserCoupons(UUID userId, String status, int page, int size);
    ApplyCouponResult applyCouponPreview(UUID couponId, UUID userId, BigDecimal orderAmount);

    // ---- Internal (order-service) ----
    ValidateCouponResult validateCoupon(UUID couponId, UUID userId, BigDecimal orderAmount);
    ValidateCouponResult validateSellerCoupon(UUID couponId, UUID userId,UUID sellerId, BigDecimal orderAmount);
    ApplyCouponResult applyCoupon(UUID couponId, UUID userId, UUID orderId, BigDecimal orderAmount);

    /**
     * Apply a platform coupon that was pro-rated across multiple per-seller orders.
     * Marks the user's claim as USED once, increments coupon.usedQuantity once, and
     * persists one usage-history row per (orderId, share). Idempotent on (couponId,userId,orderId).
     */
    void applyPlatformCouponMulti(UUID couponId, UUID userId, List<OrderShare> allocations);

    void releaseCoupon(UUID couponId, UUID userId, UUID orderId);

    record OrderShare(UUID orderId, BigDecimal share) {}

    // ---- Commands ----
    record CreateCouponCommand(UUID campaignId, UUID sellerId, String code, String couponType,
                                String discountType, BigDecimal discountValue, BigDecimal minOrderAmount,
                                BigDecimal maxDiscountAmount, int totalQuantity, int maxUsagePerUser,
                                boolean isStackable, String startDate, String endDate,
                                List<RuleItem> rules) {}

    record UpdateCouponCommand(String code, String discountType, BigDecimal discountValue,
                                BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                Integer totalQuantity, Integer maxUsagePerUser, Boolean isStackable,
                                String status, String startDate, String endDate) {}

    record RuleItem(RuleType ruleType, UUID targetId) {}

    record ValidateCouponResult(boolean valid, String message, BigDecimal discountAmount,
                                 String couponCode, String discountType) {}

    record ApplyCouponResult(UUID couponId, String couponCode, BigDecimal discountAmount,
                              String discountType, BigDecimal originalAmount, BigDecimal finalAmount) {}
}

