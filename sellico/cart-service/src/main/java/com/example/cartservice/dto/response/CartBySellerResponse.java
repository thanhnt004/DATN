package com.example.cartservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartBySellerResponse {
    private UUID sellerId;
    private String sellerName;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private Integer itemCount;
    private Boolean allSelected;
}

