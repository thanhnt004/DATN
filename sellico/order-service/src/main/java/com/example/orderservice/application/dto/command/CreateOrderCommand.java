package com.example.orderservice.application.dto.command;

import com.example.orderservice.domain.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateOrderCommand {
    @NotNull
    private UUID userId;

    @NotNull
    private UUID checkoutSessionId;

    // Payment
    @NotNull
    private PaymentMethod paymentMethod;

    // Buyer info (from JWT)
    private String buyerEmail;
    private String buyerName;
}

