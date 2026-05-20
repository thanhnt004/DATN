package com.example.orderservice.application.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutSessionResponse {
    UUID sessionId;
    List<CheckoutItemResponse> items;
    ShippingAddressResponse shippingAddress;
    List<SellerSessionResponse> sellerSessions;
    UUID voucherId;
    UUID cartId;
    BigDecimal totalAmount;
    BigDecimal shippingFee;
    BigDecimal discount;
    BigDecimal finalAmount;
    Instant expiredAt;
}
