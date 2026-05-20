package com.example.paymentservice.adapter.in.web.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String vnpayTxnRef;
    private String vnpayTransactionNo;
    private String bankCode;
    private String cardType;
    private String payDate;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}

