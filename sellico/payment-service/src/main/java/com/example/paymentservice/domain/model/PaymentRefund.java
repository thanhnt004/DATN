package com.example.paymentservice.domain.model;

import com.example.paymentservice.domain.model.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRefund {
    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private String vnpayTxnRef;
    private Instant createdAt;
    private Instant updatedAt;
}

