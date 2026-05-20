package com.example.paymentservice.domain.model;

import com.example.paymentservice.domain.model.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private PaymentStatus status;
    private String vnpayTxnRef;
    private String vnpayTransactionNo;
    private String bankCode;
    private String cardType;
    private String payDate;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static Payment createPending(UUID orderId, UUID userId, BigDecimal amount, String vnpayTxnRef) {
        Instant now = Instant.now();
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency("VND")
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .vnpayTxnRef(vnpayTxnRef)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Factory: create a COD (Cash On Delivery) payment record.
     * Immediately in COD_PENDING status — awaiting delivery & cash collection.
     */
    public static Payment createCod(UUID orderId, UUID userId, BigDecimal amount, String shippingAddress) {
        Instant now = Instant.now();
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency("VND")
                .paymentMethod("COD")
                .status(PaymentStatus.COD_PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void markCompleted(String vnpayTransactionNo, String bankCode, String cardType, String payDate) {
        this.status = PaymentStatus.COMPLETED;
        this.vnpayTransactionNo = vnpayTransactionNo;
        this.bankCode = bankCode;
        this.cardType = cardType;
        this.payDate = payDate;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        this.status = PaymentStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isCod() {
        return "COD".equals(this.paymentMethod);
    }

    public boolean isCodPending() {
        return this.status == PaymentStatus.COD_PENDING;
    }

    /**
     * COD: delivery agent collected cash → mark confirmed then completed
     */
    public void markCodConfirmed() {
        this.status = PaymentStatus.COD_CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void markCodCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.payDate = Instant.now().toString();
        this.updatedAt = Instant.now();
    }

    /**
     * COD: order cancelled before delivery
     */
    public void markCodCancelled(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason != null ? reason : "COD order cancelled";
        this.updatedAt = Instant.now();
    }
}

