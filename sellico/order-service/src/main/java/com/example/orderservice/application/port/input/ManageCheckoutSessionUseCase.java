package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.response.CheckoutSessionResponse;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;

import java.util.UUID;

public interface ManageCheckoutSessionUseCase {
    CheckoutSessionResponse getCheckoutSession(UUID sessionId, UUID userId);
    CheckoutSessionResponse updateQuantity(UUID sessionId, UUID userId, UUID skuId, int quantity);
    CheckoutSessionResponse updateAddress(UUID sessionId, UUID userId, ShippingAddress address);
    CheckoutSessionResponse updateVoucher(UUID sessionId, UUID userId, UUID sellerId, UUID voucherId);
    CheckoutSessionResponse updatePlatformVoucher(UUID sessionId, UUID userId, UUID voucherId);
    CheckoutSessionResponse updateBuyerNote(UUID sessionId, UUID userId, UUID sellerId, String note);
}
