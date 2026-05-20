package com.example.paymentservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when payment is completed successfully.
 * Order-service listens to "payment.completed" topic and expects these fields.
 */
@Getter
@Builder
@AllArgsConstructor
public class PaymentCompletedEvent {
    private final UUID orderId;
    private final String paymentMethod;
    private final BigDecimal amount;
    private final String transactionId;
    private final String paymentGateway;
}

