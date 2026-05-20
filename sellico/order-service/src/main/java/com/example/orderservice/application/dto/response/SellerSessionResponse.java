package com.example.orderservice.application.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerSessionResponse {
    UUID sellerId;
    UUID voucherId;
    String buyerNote;
    BigDecimal totalAmount;
    BigDecimal shippingFee;
    BigDecimal discount;
    BigDecimal finalAmount;
}
