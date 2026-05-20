package com.example.discountservice.application.service;

import com.example.discountservice.application.port.in.CouponUseCase;
import com.example.discountservice.application.port.out.*;
import com.example.discountservice.domain.exception.DiscountBusinessException;
import com.example.discountservice.domain.exception.DiscountErrorCode;
import com.example.discountservice.domain.model.*;
import com.example.discountservice.domain.model.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService implements CouponUseCase {

    private final CouponRepositoryPort couponRepo;
    private final DiscountRuleRepositoryPort ruleRepo;
    private final UserCouponClaimRepositoryPort claimRepo;
    private final CouponUsageHistoryRepositoryPort usageRepo;

    // =====================================================
    // Admin / Seller CRUD
    // =====================================================

    @Override
    @Transactional
    public Coupon createCoupon(CreateCouponCommand command) {
        if (couponRepo.existsByCode(command.code())) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_CODE_EXISTS);
        }

        Instant start = Instant.parse(command.startDate());
        Instant end = Instant.parse(command.endDate());
        if (!end.isAfter(start)) {
            throw new DiscountBusinessException(DiscountErrorCode.INVALID_DATE_RANGE);
        }

        Coupon coupon = Coupon.builder()
                .campaignId(command.campaignId())
                .sellerId(command.sellerId())
                .code(command.code().toUpperCase())
                .couponType(CouponType.valueOf(command.couponType()))
                .discountType(DiscountType.valueOf(command.discountType()))
                .discountValue(command.discountValue())
                .minOrderAmount(command.minOrderAmount() != null ? command.minOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(command.maxDiscountAmount())
                .totalQuantity(command.totalQuantity())
                .claimedQuantity(0)
                .usedQuantity(0)
                .maxUsagePerUser(command.maxUsagePerUser())
                .isStackable(command.isStackable())
                .status(CouponStatus.ACTIVE)
                .startDate(start)
                .endDate(end)
                .version(0)
                .build();

        Coupon saved = couponRepo.save(coupon);

        // Save rules
        if (command.rules() != null && !command.rules().isEmpty()) {
            List<DiscountRule> rules = command.rules().stream()
                    .map(r -> DiscountRule.builder()
                            .couponId(saved.getId())
                            .ruleType(r.ruleType())
                            .targetId(r.targetId())
                            .build())
                    .toList();
            ruleRepo.saveAll(rules);
            saved.setRules(rules);
        }

        log.info("Created coupon: {} (code={})", saved.getId(), saved.getCode());
        return saved;
    }

    @Override
    @Transactional
    public Coupon updateCoupon(UUID id, UpdateCouponCommand command) {
        Coupon coupon = findCouponOrThrow(id);

        if (command.code() != null && !command.code().equalsIgnoreCase(coupon.getCode())) {
            if (couponRepo.existsByCode(command.code())) {
                throw new DiscountBusinessException(DiscountErrorCode.COUPON_CODE_EXISTS);
            }
            coupon.setCode(command.code().toUpperCase());
        }
        if (command.discountType() != null) coupon.setDiscountType(DiscountType.valueOf(command.discountType()));
        if (command.discountValue() != null) coupon.setDiscountValue(command.discountValue());
        if (command.minOrderAmount() != null) coupon.setMinOrderAmount(command.minOrderAmount());
        if (command.maxDiscountAmount() != null) coupon.setMaxDiscountAmount(command.maxDiscountAmount());
        if (command.totalQuantity() != null) coupon.setTotalQuantity(command.totalQuantity());
        if (command.maxUsagePerUser() != null) coupon.setMaxUsagePerUser(command.maxUsagePerUser());
        if (command.isStackable() != null) coupon.setIsStackable(command.isStackable());
        if (command.status() != null) coupon.setStatus(CouponStatus.valueOf(command.status()));
        if (command.startDate() != null) coupon.setStartDate(Instant.parse(command.startDate()));
        if (command.endDate() != null) coupon.setEndDate(Instant.parse(command.endDate()));

        return couponRepo.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon getCoupon(UUID id) {
        Coupon coupon = findCouponOrThrow(id);
        coupon.setRules(ruleRepo.findByCouponId(id));
        return coupon;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Coupon> listCoupons(UUID sellerId, String status, int page, int size) {
        CouponStatus cs = (status != null) ? CouponStatus.valueOf(status) : null;
        return couponRepo.findBySellerId(sellerId, cs, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Coupon> listCouponsByType(String couponType, String status, int page, int size) {
        CouponStatus cs = (status != null) ? CouponStatus.valueOf(status) : null;
        return couponRepo.findByType(couponType, cs, page, size);
    }

    // =====================================================
    // Buyer
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public Page<Coupon> listAvailableCoupons(UUID sellerId, int page, int size) {
        if (sellerId != null) {
            return couponRepo.findActiveBySellerIds(sellerId, page, size);
        }
        return couponRepo.findAllActive(page, size);
    }

    @Override
    @Transactional
    public UserCouponClaim claimCoupon(UUID couponId, UUID userId) {
        Coupon coupon = findCouponOrThrow(couponId);

        if (!coupon.isActive()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_NOT_ACTIVE);
        }
        if (coupon.isExpired()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_EXPIRED);
        }
        if (coupon.isNotStarted()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_NOT_STARTED);
        }
        if (coupon.isDepleted()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_DEPLETED);
        }

        // Check per-user claim limit
        long userClaimCount = claimRepo.countByCouponIdAndUserId(couponId, userId);
        if (userClaimCount >= coupon.getMaxUsagePerUser()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }

        // Check if already has active claim
        if (claimRepo.existsByCouponIdAndUserIdAndStatus(couponId, userId, ClaimStatus.CLAIMED)) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_ALREADY_CLAIMED);
        }

        // Atomically increment claimed_quantity
        int updated = couponRepo.incrementClaimedQuantity(couponId);
        if (updated == 0) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_DEPLETED);
        }

        UserCouponClaim claim = UserCouponClaim.builder()
                .couponId(couponId)
                .userId(userId)
                .status(ClaimStatus.CLAIMED)
                .claimedAt(Instant.now())
                .build();

        UserCouponClaim saved = claimRepo.save(claim);
        log.info("User {} claimed coupon {} (claim={})", userId, couponId, saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCouponClaim> getUserCoupons(UUID userId, String status, int page, int size) {
        ClaimStatus cs;
        if (status == null || "AVAILABLE".equalsIgnoreCase(status)) {
            cs = ClaimStatus.CLAIMED;
        } else {
            cs = ClaimStatus.valueOf(status);
        }
        return claimRepo.findByUserId(userId, cs, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyCouponResult applyCouponPreview(UUID couponId, UUID userId, BigDecimal orderAmount) {
        Coupon coupon = findCouponOrThrow(couponId);
        validateCouponForUser(coupon, userId, orderAmount);

        BigDecimal discount = coupon.calculateDiscount(orderAmount);
        return new ApplyCouponResult(
                coupon.getId(), coupon.getCode(), discount,
                coupon.getDiscountType().name(), orderAmount, orderAmount.subtract(discount));
    }

    // =====================================================
    // Internal (order-service)
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public ValidateCouponResult validateCoupon(UUID couponId, UUID userId, BigDecimal orderAmount) {
        try {
            Coupon coupon = findCouponOrThrow(couponId);
            validateCouponForUser(coupon, userId, orderAmount);
            BigDecimal discount = coupon.calculateDiscount(orderAmount);

            return new ValidateCouponResult(true, "Valid", discount,
                    coupon.getCode(), coupon.getDiscountType().name());
        } catch (DiscountBusinessException e) {
            return new ValidateCouponResult(false, e.getErrorCode().getMessage(),
                    BigDecimal.ZERO, null, null);
        }
    }

    @Override
    public ValidateCouponResult validateSellerCoupon(UUID couponId, UUID userId, UUID sellerId, BigDecimal orderAmount) {
        try {
            Coupon coupon = findCouponOrThrow(couponId);
            validateCouponForSeller(coupon, userId, orderAmount,sellerId);
            BigDecimal discount = coupon.calculateDiscount(orderAmount);
            return new ValidateCouponResult(true, "Valid", discount,
                    coupon.getCode(), coupon.getDiscountType().name());
        }catch (DiscountBusinessException e) {
            return new ValidateCouponResult(false, e.getErrorCode().getMessage(),
                    BigDecimal.ZERO, null, null);
        }
    }



    @Override
    @Transactional
    public ApplyCouponResult applyCoupon(UUID couponId, UUID userId, UUID orderId, BigDecimal orderAmount) {
        Coupon coupon = findCouponOrThrow(couponId);
        validateCouponForUser(coupon, userId, orderAmount);

        BigDecimal discount = coupon.calculateDiscount(orderAmount);

        // Find user's active claim
        UserCouponClaim claim = claimRepo.findByCouponIdAndUserIdAndStatus(couponId, userId, ClaimStatus.CLAIMED)
                .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.CLAIM_NOT_FOUND));

        // Mark claim as used
        claim.setStatus(ClaimStatus.USED);
        claim.setUsedAt(Instant.now());
        claimRepo.save(claim);

        // Increment used_quantity
        couponRepo.incrementUsedQuantity(couponId);

        // Save usage history
        CouponUsageHistory history = CouponUsageHistory.builder()
                .claimId(claim.getId())
                .couponId(couponId)
                .userId(userId)
                .orderId(orderId)
                .discountAmount(discount)
                .usedAt(Instant.now())
                .build();
        usageRepo.save(history);

        log.info("Coupon {} applied for order {} (discount={})", couponId, orderId, discount);
        return new ApplyCouponResult(coupon.getId(), coupon.getCode(), discount,
                coupon.getDiscountType().name(), orderAmount, orderAmount.subtract(discount));
    }

    @Override
    @Transactional
    public void applyPlatformCouponMulti(UUID couponId, UUID userId, List<OrderShare> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }

        findCouponOrThrow(couponId);

        // Reuse the active claim if present; if it has already been marked USED for
        // this checkout (a redelivered event), only top-up missing per-order rows.
        UserCouponClaim claim = claimRepo.findByCouponIdAndUserIdAndStatus(couponId, userId, ClaimStatus.CLAIMED)
                .orElseGet(() -> claimRepo.findByCouponIdAndUserIdAndStatus(couponId, userId, ClaimStatus.USED)
                        .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.CLAIM_NOT_FOUND)));

        boolean firstApply = claim.getStatus() == ClaimStatus.CLAIMED;
        if (firstApply) {
            claim.setStatus(ClaimStatus.USED);
            claim.setUsedAt(Instant.now());
            claimRepo.save(claim);
            couponRepo.incrementUsedQuantity(couponId);
        }

        Instant now = Instant.now();
        for (OrderShare alloc : allocations) {
            if (alloc.share() == null || alloc.share().signum() <= 0) continue;
            // Idempotency: skip rows already persisted from a previous delivery.
            if (usageRepo.existsByCouponIdAndOrderId(couponId, alloc.orderId())) continue;

            CouponUsageHistory history = CouponUsageHistory.builder()
                    .claimId(claim.getId())
                    .couponId(couponId)
                    .userId(userId)
                    .orderId(alloc.orderId())
                    .discountAmount(alloc.share())
                    .usedAt(now)
                    .build();
            usageRepo.save(history);
        }

        log.info("Platform coupon {} applied across {} orders for user {} (firstApply={})",
                couponId, allocations.size(), userId, firstApply);
    }

    @Override
    @Transactional
    public void releaseCoupon(UUID couponId, UUID userId, UUID orderId) {
        UserCouponClaim claim = claimRepo.findByCouponIdAndUserIdAndStatus(couponId, userId, ClaimStatus.USED)
                .orElse(null);

        if (claim != null) {
            claim.setStatus(ClaimStatus.RELEASED);
            claimRepo.save(claim);
            couponRepo.decrementUsedQuantity(couponId);
            usageRepo.deleteByOrderId(orderId);
            log.info("Released coupon {} for order {} (user={})", couponId, orderId, userId);
        }
    }

    // =====================================================
    // Helpers
    // =====================================================

    private Coupon findCouponOrThrow(UUID id) {
        return couponRepo.findById(id)
                .orElseThrow(() -> new DiscountBusinessException(DiscountErrorCode.COUPON_NOT_FOUND));
    }

    private void validateCouponForUser(Coupon coupon, UUID userId, BigDecimal orderAmount) {
        if (!coupon.isActive()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_NOT_ACTIVE);
        }
        if (coupon.isExpired()) {
            throw new DiscountBusinessException(DiscountErrorCode.COUPON_EXPIRED);
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new DiscountBusinessException(DiscountErrorCode.MIN_ORDER_NOT_MET);
        }
        // Check if user has claimed this coupon
        if (!claimRepo.existsByCouponIdAndUserIdAndStatus(coupon.getId(), userId, ClaimStatus.CLAIMED)) {
            throw new DiscountBusinessException(DiscountErrorCode.CLAIM_NOT_FOUND);
        }
    }
    private void validateCouponForSeller(Coupon coupon, UUID userId, BigDecimal orderAmount, UUID sellerId) {

        validateCouponForUser(coupon, userId, orderAmount);
        if (!coupon.getSellerId().equals(sellerId)) {
            throw new DiscountBusinessException(DiscountErrorCode.SELLER_MISMATCH);
        }
    }
}

