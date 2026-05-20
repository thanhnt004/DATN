package com.example.discountservice.adapter.out.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private UUID id;
    private UUID userId;
}
