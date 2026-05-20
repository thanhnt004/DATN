package com.example.orderservice.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CancelOrderCommand {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID userId;

    @NotBlank
    private String reason;

    private boolean isSeller;
}

