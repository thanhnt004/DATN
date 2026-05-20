package com.example.paymentservice.adapter.in.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1", message = "Amount must be positive")
    private BigDecimal amount;

    private String reason;
}

