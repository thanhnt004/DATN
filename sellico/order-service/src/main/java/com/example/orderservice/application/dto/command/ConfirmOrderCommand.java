package com.example.orderservice.application.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ConfirmOrderCommand {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID sellerId;

    private String note;
}

