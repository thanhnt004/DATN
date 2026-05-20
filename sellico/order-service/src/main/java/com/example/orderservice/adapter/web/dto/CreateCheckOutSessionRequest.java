package com.example.orderservice.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateCheckOutSessionRequest {

    @NotEmpty
    @Valid
    private List<ItemDto> items;

    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientPhone;

    @NotBlank
    private String shippingAddress;

    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;
    private UUID cartId;

    private Map<UUID,String> buyerNotes;

    private Map<UUID,UUID> sellerVoucherID;

    private UUID voucherId;
}
