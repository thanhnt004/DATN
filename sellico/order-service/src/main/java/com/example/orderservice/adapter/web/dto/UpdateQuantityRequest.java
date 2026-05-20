package com.example.orderservice.adapter.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateQuantityRequest {
    @NotNull
    private UUID skuId;

    @Min(1)
    private int quantity;
}
