package com.example.inventoryservice.dto.response;

import com.example.inventoryservice.entity.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private UUID id;
    private UUID skuId;
    private UUID orderId;
    private Integer quantity;
    private ReservationStatus status;
    private Instant expiresAt;
    private Instant createdAt;
}

