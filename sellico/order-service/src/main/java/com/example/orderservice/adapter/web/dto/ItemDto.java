package com.example.orderservice.adapter.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class ItemDto {
    @NotNull
    private UUID skuId;

    @NotNull
    @Min(1)
    private Integer quantity;
}