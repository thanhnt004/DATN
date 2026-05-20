package com.example.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CreateCodPaymentCommand {
    private final UUID orderId;
    private final UUID userId;
    private final BigDecimal amount;
    private final String shippingAddress;
}

