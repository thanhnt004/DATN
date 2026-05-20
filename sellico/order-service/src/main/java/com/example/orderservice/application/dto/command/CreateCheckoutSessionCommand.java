package com.example.orderservice.application.dto.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CreateCheckoutSessionCommand {

    @NotEmpty
    @Valid
    private List<ItemCommand> items;
    // Shipping address
    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientPhone;

    @NotBlank
    private String shippingAddress;

    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;

    private UUID voucherId;
    private Map<UUID,String> buyerNotes;

    private Map<UUID,UUID> sellerVoucherID;
    private UUID cartId;
    private UUID userId;
    private String userName;
    private String email;
}
