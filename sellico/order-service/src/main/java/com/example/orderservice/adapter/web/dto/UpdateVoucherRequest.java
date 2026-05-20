package com.example.orderservice.adapter.web.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateVoucherRequest {
    private UUID voucherId;
}
