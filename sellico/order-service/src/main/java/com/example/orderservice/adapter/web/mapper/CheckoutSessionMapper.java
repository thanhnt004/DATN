package com.example.orderservice.adapter.web.mapper;

import com.example.orderservice.adapter.web.dto.CreateCheckOutSessionRequest;
import com.example.orderservice.adapter.web.dto.ItemDto;
import com.example.orderservice.application.dto.command.CreateCheckoutSessionCommand;
import com.example.orderservice.application.dto.command.ItemCommand;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CheckoutSessionMapper {
    public CreateCheckoutSessionCommand toCommand(CreateCheckOutSessionRequest request, UUID userId,String userName,String email) {
        return CreateCheckoutSessionCommand.builder()
                .userId(userId)
                .userName(userName)
                .email(email)
                .sellerVoucherID(request.getSellerVoucherID())
                .buyerNotes(request.getBuyerNotes())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingDistrict(request.getShippingDistrict())
                .shippingWard(request.getShippingWard())
                .cartId(request.getCartId())
                .voucherId(request.getVoucherId())
                .items(request.getItems().stream()
                        .map(this::toItemCommand)
                        .toList())
                .build();
    }
    private ItemCommand toItemCommand(ItemDto dto) {
        return ItemCommand.builder()
                .skuId(dto.getSkuId())
                .quantity(dto.getQuantity())
                .build();
    }
}
