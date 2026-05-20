package com.example.orderservice.domain.model;

import com.example.orderservice.domain.exception.OrderDomainException;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSession {

    UUID id;

    UUID userId;

    List<SellerSession>  sellerSessions;

    Money totalAmount;
    Money shippingFee;
    Money discountAmount;
    Money finalAmount;
    ShippingAddress shippingAddress;

    UUID voucherId;//flatform
    UUID cartId;

    Instant expiredAt;

    String status; // ACTIVE, USED, EXPIRED
    public static CheckoutSession create(UUID userId, ShippingAddress shippingAddress,
                                 UUID voucherId) {
        return create(userId, shippingAddress, voucherId, null);
    }

    public static CheckoutSession create(UUID userId, ShippingAddress shippingAddress,
                                 UUID voucherId, UUID cartId) {
        return CheckoutSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status("ACTIVE")
                .shippingAddress(shippingAddress)
                .voucherId(voucherId)
                .cartId(cartId)
                .sellerSessions(new java.util.ArrayList<>())
                .totalAmount(Money.ZERO)
                .shippingFee(Money.ZERO)
                .discountAmount(Money.ZERO)
                .finalAmount(Money.ZERO)
                .expiredAt(Instant.now().plusSeconds(900))
                .build();
    }
    public void addSellerSession(SellerSession sellerSession) {
        this.sellerSessions.add(sellerSession);
        recalculateTotals();
    }
    public void setShippingFee(Money shippingFee) {
        this.shippingFee = shippingFee;
        recalculateTotals();
    }

    public void applyDiscount(Money discountAmount) {
        this.discountAmount = discountAmount;
        recalculateTotals();
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public void setVoucherId(UUID voucherId) {
        this.voucherId = voucherId;
    }

    public void recalculateTotals() {
        this.totalAmount = sellerSessions.stream()
                .map(SellerSession::getFinalAmount)
                .reduce(Money.ZERO, Money::add);

        this.finalAmount = totalAmount
                .add(shippingFee != null ? shippingFee : Money.ZERO)
                .subtract(discountAmount != null ? discountAmount : Money.ZERO);
    }

    private void validateCanModify() {
        if (!Objects.equals(status, "ACTIVE")) {
            throw new OrderDomainException("Cannot modify order in status: " + status);
        }

    }

}
