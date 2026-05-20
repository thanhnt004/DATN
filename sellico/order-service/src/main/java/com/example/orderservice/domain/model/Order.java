package com.example.orderservice.domain.model;

import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.example.orderservice.domain.model.enums.PaymentStatus;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.model.valueobject.ShippingAddress;
import com.example.orderservice.domain.exception.OrderDomainException;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order Aggregate Root - Domain Entity
 */
@Getter
@Builder
public class Order {
    private OrderId id;
    private String orderNumber;
    private UUID userId;
    private UUID sellerId;
    private OrderStatus status;

    // Pricing
    private Money subtotal;
    private Money shippingFee;
    private Money discountAmount;
    private Money totalAmount;

    // Shipping
    private ShippingAddress shippingAddress;

    // Payment
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Instant paidAt;

    // Shipping tracking
    private String shippingProvider;
    private String trackingNumber;
    private Instant shippedAt;
    private Instant deliveredAt;

    // Notes
    private String buyerNote;
    private String sellerNote;
    private String cancelReason;
    private String failureReason;
    private String voucherCode;

    // Platform voucher (pro-rated across orders that share the same platform coupon)
    private UUID platformCouponId;
    private Money platformVoucherShare;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    // Items
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // Version for optimistic locking
    private Long version;

    // =====================================================
    // Domain Business Logic
    // =====================================================

    public static Order create(UUID userId, UUID sellerId, ShippingAddress shippingAddress,
                                PaymentMethod paymentMethod, String buyerNote, String voucherCode) {
        return Order.builder()
                .id(new OrderId(UUID.randomUUID()))
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .sellerId(sellerId)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .buyerNote(buyerNote)
                .voucherCode(voucherCode)
                .subtotal(Money.ZERO)
                .shippingFee(Money.ZERO)
                .discountAmount(Money.ZERO)
                .platformVoucherShare(Money.ZERO)
                .totalAmount(Money.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }

    private static String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    public void addItem(OrderItem item) {
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

    public void applyPlatformVoucher(UUID platformCouponId, Money share) {
        this.platformCouponId = platformCouponId;
        this.platformVoucherShare = share != null ? share : Money.ZERO;
        recalculateTotals();
    }

    private void recalculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(Money.ZERO, Money::add);

        Money sellerDiscount = discountAmount != null ? discountAmount : Money.ZERO;
        Money platformShare = platformVoucherShare != null ? platformVoucherShare : Money.ZERO;

        this.totalAmount = subtotal
                .add(shippingFee != null ? shippingFee : Money.ZERO)
                .subtract(sellerDiscount)
                .subtract(platformShare);
    }

    // Status transitions
    public void markPaid(Instant paidAt) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        validateStatus(OrderStatus.PENDING);
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void ship(String shippingProvider, String trackingNumber) {
        validateStatus(OrderStatus.CONFIRMED);
        this.status = OrderStatus.SHIPPED;
        this.shippingProvider = shippingProvider;
        this.trackingNumber = trackingNumber;
        this.shippedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void deliver() {
        validateStatus(OrderStatus.SHIPPED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void complete() {
        validateStatus(OrderStatus.DELIVERED);
        this.status = OrderStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        validateCanCancel();
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Validation methods
    private void validateCanModify() {
        if (status != OrderStatus.PENDING) {
            throw new OrderDomainException("Cannot modify order in status: " + status);
        }
    }

    private void validateStatus(OrderStatus... expectedStatuses) {
        for (OrderStatus expected : expectedStatuses) {
            if (this.status == expected) {
                return;
            }
        }
        throw new OrderDomainException(
                "Invalid status transition from " + status + ". Expected: " + java.util.Arrays.toString(expectedStatuses));
    }

    private void validateCanCancel() {
        if (status == OrderStatus.SHIPPED ||
            status == OrderStatus.DELIVERED ||
            status == OrderStatus.COMPLETED ||
            status == OrderStatus.CANCELLED) {
            throw new OrderDomainException("Cannot cancel order in status: " + status);
        }
    }

    public boolean canCancel() {
        return status != OrderStatus.SHIPPED &&
               status != OrderStatus.DELIVERED &&
               status != OrderStatus.COMPLETED &&
               status != OrderStatus.CANCELLED;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isCOD() {
        return paymentMethod == PaymentMethod.COD;
    }
}

