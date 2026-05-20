package com.example.orderservice.adapter.web.mapper;

import com.example.orderservice.adapter.web.dto.CancelOrderRequestDto;
import com.example.orderservice.adapter.web.dto.CreateOrderRequestDto;
import com.example.orderservice.adapter.web.dto.ItemDto;
import com.example.orderservice.adapter.web.dto.ShipOrderRequestDto;
import com.example.orderservice.application.dto.command.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderWebMapper {

    public CreateOrderCommand toCommand(CreateOrderRequestDto dto, UUID userId, String email, String name) {
        return CreateOrderCommand.builder()
                .userId(userId)
                .checkoutSessionId(dto.getCheckoutSessionId())
                .paymentMethod(dto.getPaymentMethod())
                .buyerEmail(email)
                .buyerName(name)
                .build();
    }

    private ItemCommand toItemCommand(ItemDto dto) {
        return ItemCommand.builder()
                .skuId(dto.getSkuId())
                .quantity(dto.getQuantity())
                .build();
    }

    public CancelOrderCommand toCancelCommand(UUID orderId, UUID userId, CancelOrderRequestDto dto, boolean isSeller) {
        return CancelOrderCommand.builder()
                .orderId(orderId)
                .userId(userId)
                .reason(dto.getReason())
                .isSeller(isSeller)
                .build();
    }

    public ConfirmOrderCommand toConfirmCommand(UUID orderId, UUID sellerId, String note) {
        return ConfirmOrderCommand.builder()
                .orderId(orderId)
                .sellerId(sellerId)
                .note(note)
                .build();
    }

    public ShipOrderCommand toShipCommand(UUID orderId, UUID sellerId, ShipOrderRequestDto dto) {
        return ShipOrderCommand.builder()
                .orderId(orderId)
                .sellerId(sellerId)
                .shippingProvider(dto.getShippingProvider())
                .trackingNumber(dto.getTrackingNumber())
                .note(dto.getNote())
                .build();
    }
}

