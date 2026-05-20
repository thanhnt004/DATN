package com.example.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class RefundPaymentCommand {
    private final UUID paymentId;
    private final BigDecimal amount;
    private final String reason;
    private final UUID userId;
}

