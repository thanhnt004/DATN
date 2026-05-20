package com.example.orderservice.domain.model;

import com.example.orderservice.domain.exception.OrderDomainException;
import com.example.orderservice.domain.model.valueobject.Money;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerSession {

    UUID sellerId;

    List<CheckoutItem> items;

    Money totalAmount;
    Money shippingFee;
    Money discountAmount;

    Money finalAmount;

    UUID voucherId; // theo shop
    String buyerNote;

    String status; // ACTIVE, FAILED, RESERVED
    public static SellerSession create(UUID sellerId,UUID voucherId,String buyerNote) {
        return SellerSession.builder()
                .sellerId(sellerId)
                .items(new java.util.ArrayList<>())
                .totalAmount(Money.ZERO)
                .shippingFee(Money.ZERO)
                .discountAmount(Money.ZERO)
                .finalAmount(Money.ZERO)
                .voucherId(voucherId)
                .buyerNote(buyerNote)
                .status("ACTIVE")
                .build();

    }
    public void addItem(CheckoutItem item) {
        validateCanModify();
        items.add(item);
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

    public void setVoucherId(UUID voucherId) {
        this.voucherId = voucherId;
    }

    public void setBuyerNote(String buyerNote) {
        this.buyerNote = buyerNote;
    }

    public void recalculateTotals() {
        this.totalAmount = items.stream()
                .map(CheckoutItem::getSubtotal)
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
