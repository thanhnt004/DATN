package com.example.orderservice.adapter.web.dto;

import com.example.orderservice.domain.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderRequestDto {

    @NotNull
    private UUID checkoutSessionId;

    @NotNull
    private PaymentMethod paymentMethod;
}

