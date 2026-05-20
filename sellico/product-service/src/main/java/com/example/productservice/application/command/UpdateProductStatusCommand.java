package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateProductStatusCommand {
    UUID productId;
    UUID sellerId;  // For authorization check (can be null for admin)
    String status;  // DRAFT, PENDING, ACTIVE, BANNED, DELETED
}
