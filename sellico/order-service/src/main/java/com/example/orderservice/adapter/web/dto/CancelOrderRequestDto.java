package com.example.orderservice.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelOrderRequestDto {

    @NotBlank
    private String reason;
}

