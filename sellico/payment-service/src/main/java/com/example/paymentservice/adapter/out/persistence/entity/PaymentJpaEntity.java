package com.example.paymentservice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_vnpay_txn_ref", columnList = "vnpay_txn_ref")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "payment_method", length = 30, nullable = false)
    @Builder.Default
    private String paymentMethod = "VNPAY";

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "vnpay_txn_ref", length = 100, unique = true)
    private String vnpayTxnRef;

    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    @Column(name = "bank_code", length = 30)
    private String bankCode;

    @Column(name = "card_type", length = 30)
    private String cardType;

    @Column(name = "pay_date", length = 20)
    private String payDate;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

