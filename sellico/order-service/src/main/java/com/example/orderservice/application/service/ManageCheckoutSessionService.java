package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.response.CheckoutItemResponse;
import com.example.orderservice.application.dto.response.CheckoutSessionResponse;
import com.example.orderservice.application.dto.response.SellerSessionResponse;
import com.example.orderservice.application.dto.response.ShippingAddressResponse;
import com.example.orderservice.application.port.input.ManageCheckoutSessionUseCase;
import com.example.orderservice.domain.exception.OrderDomainException;
import com.example.orderservice.domain.model.CheckoutItem;
import com.example.orderservice.domain.model.CheckoutSession;
import com.example.orderservice.domain.model.SellerSession;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import com.example.orderservice.infrastructure.client.DiscountFeignClient;
import com.example.orderservice.infrastructure.client.InventoryFeignClient;
import com.example.orderservice.infrastructure.client.ShippingFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManageCheckoutSessionService implements ManageCheckoutSessionUseCase {
    private final ObjectMapper objectMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final ShippingFeignClient shippingFeignClient;
    private final DiscountFeignClient discountFeignClient;
    private final CheckoutSessionRedisService checkoutSessionRedisService;

    @Override
    public CheckoutSessionResponse getCheckoutSession(UUID sessionId, UUID userId) {
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);
        return toResponse(checkoutSession);
    }

    @Override
    public CheckoutSessionResponse updateQuantity(UUID sessionId, UUID userId, UUID skuId, int quantity) {
        if (quantity < 1) {
            throw new OrderDomainException("Quantity must be at least 1");
        }
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);

        CheckoutItem item = findCheckoutItem(checkoutSession, skuId);
        InventoryFeignClient.StockAvailabilityResponse stockAvailability = inventoryFeignClient.getInventories(List.of(skuId))
                .getData()
                .stream()
                .findFirst()
                .orElseThrow(() -> new OrderDomainException("Inventory information unavailable for sku: " + skuId));

        item.updateQuantity(quantity, stockAvailability);
        SellerSession sellerSession = findSellerSessionForItem(checkoutSession, skuId);
        recalculateSellerSession(checkoutSession, sellerSession);
        applyPlatformVoucher(checkoutSession);
        checkoutSession.recalculateTotals();
        saveCheckoutSession(checkoutSession);
        return toResponse(checkoutSession);
    }

    @Override
    public CheckoutSessionResponse updateAddress(UUID sessionId, UUID userId, ShippingAddress address) {
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);

        checkoutSession.setShippingAddress(address);
        for (SellerSession sellerSession : checkoutSession.getSellerSessions()) {
            recalculateSellerShippingFee(sellerSession, checkoutSession.getShippingAddress());
        }
        applyPlatformVoucher(checkoutSession);
        checkoutSession.recalculateTotals();
        saveCheckoutSession(checkoutSession);
        return toResponse(checkoutSession);
    }

    @Override
    public CheckoutSessionResponse updateVoucher(UUID sessionId, UUID userId, UUID sellerId, UUID voucherId) {
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);

        SellerSession sellerSession = findSellerSession(checkoutSession, sellerId);
        sellerSession.setVoucherId(voucherId);
        applySellerVoucher(checkoutSession, sellerSession);
        applyPlatformVoucher(checkoutSession);
        checkoutSession.recalculateTotals();
        saveCheckoutSession(checkoutSession);
        return toResponse(checkoutSession);
    }

    @Override
    public CheckoutSessionResponse updatePlatformVoucher(UUID sessionId, UUID userId, UUID voucherId) {
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);

        checkoutSession.setVoucherId(voucherId);
        applyPlatformVoucher(checkoutSession);
        checkoutSession.recalculateTotals();
        saveCheckoutSession(checkoutSession);
        return toResponse(checkoutSession);
    }

    @Override
    public CheckoutSessionResponse updateBuyerNote(UUID sessionId, UUID userId, UUID sellerId, String note) {
        CheckoutSession checkoutSession = loadCheckoutSession(sessionId);
        validateOwnership(checkoutSession, userId);

        SellerSession sellerSession = findSellerSession(checkoutSession, sellerId);
        sellerSession.setBuyerNote(note != null ? note : "");
        checkoutSession.recalculateTotals();
        saveCheckoutSession(checkoutSession);
        return toResponse(checkoutSession);
    }

    private CheckoutSession loadCheckoutSession(UUID sessionId) {
        Object raw = checkoutSessionRedisService.getSession(sessionId.toString());
        if (raw == null) {
            throw new OrderDomainException("Checkout session not found: " + sessionId);
        }
        log.info("Raw checkout session data from Redis for sessionId {}: {}", sessionId, raw);
        try {
            if (raw instanceof CheckoutSession) {
                // Already deserialized
                return (CheckoutSession) raw;
            } else if (raw instanceof java.util.Map) {
                // For old data without @class, serialize back to JSON and read with proper typing
                String json = objectMapper.writeValueAsString(raw);
                return objectMapper.readValue(json, CheckoutSession.class);
            } else {
                return objectMapper.convertValue(raw, CheckoutSession.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse checkout session data for sessionId {}: {}", sessionId, e.getMessage(), e);
            throw new OrderDomainException("Unable to parse checkout session data", e);
        }
    }

    private void saveCheckoutSession(CheckoutSession checkoutSession) {
        try {
            checkoutSessionRedisService.saveSession(checkoutSession.getId().toString(), checkoutSession);
        } catch (Exception e) {
            throw new OrderDomainException("Failed to persist checkout session", e);
        }
    }

    private void validateOwnership(CheckoutSession checkoutSession, UUID userId) {
        if (!checkoutSession.getUserId().equals(userId)) {
            throw new OrderDomainException("Unauthorized access to checkout session");
        }
    }

    private CheckoutItem findCheckoutItem(CheckoutSession checkoutSession, UUID skuId) {
        return checkoutSession.getSellerSessions().stream()
                .flatMap(seller -> seller.getItems().stream())
                .filter(item -> item.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new OrderDomainException("Item not found in checkout session: " + skuId));
    }

    private SellerSession findSellerSessionForItem(CheckoutSession checkoutSession, UUID skuId) {
        return checkoutSession.getSellerSessions().stream()
                .filter(seller -> seller.getItems().stream().anyMatch(item -> item.getSkuId().equals(skuId)))
                .findFirst()
                .orElseThrow(() -> new OrderDomainException("Seller session not found for item: " + skuId));
    }

    private SellerSession findSellerSession(CheckoutSession checkoutSession, UUID sellerId) {
        return checkoutSession.getSellerSessions().stream()
                .filter(seller -> seller.getSellerId().equals(sellerId))
                .findFirst()
                .orElseThrow(() -> new OrderDomainException("Seller session not found: " + sellerId));
    }

    private void applySellerVoucher(CheckoutSession checkoutSession, SellerSession sellerSession) {
        sellerSession.applyDiscount(Money.ZERO);
        if (sellerSession.getVoucherId() == null) {
            return;
        }

        var coupon = discountFeignClient.validateCouponForSeller(DiscountFeignClient.ValidateCouponRequest.builder()
                .couponId(sellerSession.getVoucherId())
                .userId(checkoutSession.getUserId())
                .orderAmount(sellerSession.getTotalAmount().amount())
                .sellerId(sellerSession.getSellerId())
                .build())
                .getData();
        if (coupon == null || !coupon.valid()) {
            sellerSession.setVoucherId(null);
            sellerSession.applyDiscount(Money.ZERO);
        } else {
            sellerSession.applyDiscount(Money.of(coupon.discountAmount()));
        }
    }

    private void applyPlatformVoucher(CheckoutSession checkoutSession) {
        checkoutSession.applyDiscount(Money.ZERO);
        if (checkoutSession.getVoucherId() == null) {
            return;
        }

        var coupon = discountFeignClient.validateCoupon(DiscountFeignClient.ValidateCouponRequest.builder()
                .couponId(checkoutSession.getVoucherId())
                .userId(checkoutSession.getUserId())
                .orderAmount(checkoutSession.getTotalAmount().amount())
                .build())
                .getData();
        if (coupon == null || !coupon.valid()) {
            checkoutSession.setVoucherId(null);
            checkoutSession.applyDiscount(Money.ZERO);
        } else {
            checkoutSession.applyDiscount(Money.of(coupon.discountAmount()));
        }
    }

    private void recalculateSellerSession(CheckoutSession checkoutSession, SellerSession sellerSession) {
        applySellerVoucher(checkoutSession, sellerSession);
        recalculateSellerShippingFee(sellerSession, checkoutSession.getShippingAddress());
    }

    private void recalculateSellerShippingFee(SellerSession sellerSession, ShippingAddress shippingAddress) {
        int totalWeight = sellerSession.getItems().stream().mapToInt(CheckoutItem::getWeightGram).sum();
        int totalLength = sellerSession.getItems().stream().mapToInt(CheckoutItem::getLengthCm).sum();
        int totalWidth = sellerSession.getItems().stream().mapToInt(CheckoutItem::getWidthCm).sum();
        int totalHeight = sellerSession.getItems().stream().mapToInt(CheckoutItem::getHeightCm).sum();
        if (totalWeight == 0) {
            sellerSession.setShippingFee(Money.ZERO);
            return;
        }
        int shippingFee = shippingFeignClient.calculateFee(ShippingFeignClient.ShippingFeeRequest.create(
                shippingAddress,
                sellerSession.getSellerId(),
                totalWeight,
                totalLength,
                totalWidth,
                totalHeight))
                .getTotal();
        sellerSession.setShippingFee(Money.of(shippingFee));
    }

    private CheckoutSessionResponse toResponse(CheckoutSession checkoutSession) {
        List<CheckoutItemResponse> items = new ArrayList<>();
        checkoutSession.getSellerSessions().forEach(seller -> seller.getItems().forEach(item ->
                items.add(CheckoutItemResponse.builder()
                        .sellerId(seller.getSellerId())
                        .skuId(item.getSkuId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .skuCode(item.getSkuCode())
                        .imageUrl(item.getImageUrl())
                        .unitPrice(item.getUnitPrice().amount())
                        .quantity(item.getQuantity())
                        .variantInfo(item.getVariantInfo())
                        .build())));

        List<SellerSessionResponse> sellerSessions = checkoutSession.getSellerSessions().stream()
                .map(seller -> SellerSessionResponse.builder()
                        .sellerId(seller.getSellerId())
                        .voucherId(seller.getVoucherId())
                        .buyerNote(seller.getBuyerNote())
                        .totalAmount(seller.getTotalAmount().amount())
                        .shippingFee(seller.getShippingFee().amount())
                        .discount(seller.getDiscountAmount().amount())
                        .finalAmount(seller.getFinalAmount().amount())
                        .build())
                .collect(Collectors.toList());

        return CheckoutSessionResponse.builder()
                .sessionId(checkoutSession.getId())
                .items(items)
                .shippingAddress(ShippingAddressResponse.builder()
                        .recipientName(checkoutSession.getShippingAddress().recipientName())
                        .recipientPhone(checkoutSession.getShippingAddress().recipientPhone())
                        .address(checkoutSession.getShippingAddress().address())
                        .ward(checkoutSession.getShippingAddress().ward())
                        .district(checkoutSession.getShippingAddress().district())
                        .city(checkoutSession.getShippingAddress().city())
                        .fullAddress(checkoutSession.getShippingAddress().getFullAddress())
                        .build())
                .sellerSessions(sellerSessions)
                .voucherId(checkoutSession.getVoucherId())
                .cartId(checkoutSession.getCartId())
                .totalAmount(checkoutSession.getTotalAmount().amount())
                .shippingFee(checkoutSession.getShippingFee().amount())
                .discount(checkoutSession.getDiscountAmount().amount())
                .finalAmount(checkoutSession.getFinalAmount().amount())
                .expiredAt(checkoutSession.getExpiredAt())
                .build();
    }
}
