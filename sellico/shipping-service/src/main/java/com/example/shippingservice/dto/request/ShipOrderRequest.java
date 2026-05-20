package com.example.shippingservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipOrderRequest {
    private UUID sellerId;
    private String shippingProvider;
    private String trackingNumber;
    private String note;
}
