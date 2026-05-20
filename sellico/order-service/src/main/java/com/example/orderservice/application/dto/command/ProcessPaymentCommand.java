package com.example.orderservice.application.dto.command;

import com.example.orderservice.domain.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ProcessPaymentCommand {
    @NotNull
    private UUID orderId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private BigDecimal amount;

    private String transactionId;
    private String paymentGateway;
    private Map<String, Object> gatewayResponse;
}

