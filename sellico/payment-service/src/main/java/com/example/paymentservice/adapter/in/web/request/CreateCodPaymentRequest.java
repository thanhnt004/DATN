package com.example.paymentservice.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCodPaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;
    @NotNull
    private UUID userId;
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Amount must be at least 1000 VND")
    private BigDecimal amount;

    private String shippingAddress;
}

