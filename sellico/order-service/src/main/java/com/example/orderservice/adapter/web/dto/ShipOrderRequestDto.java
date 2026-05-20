package com.example.orderservice.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ShipOrderRequestDto {
    private UUID sellerId;

    @NotBlank
    private String shippingProvider;

    @NotBlank
    private String trackingNumber;

    private String note;
}

