package com.example.productservice.adapter.out.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CheckOwnerRequest {
    private UUID userId;
    private UUID sellerId;
}
