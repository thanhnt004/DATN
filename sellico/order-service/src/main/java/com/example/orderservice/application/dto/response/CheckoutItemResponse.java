package com.example.orderservice.application.dto.response;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutItemResponse {
    private UUID sellerId;

    private UUID skuId;

    private UUID productId;

    private String productName;

    private String skuCode;
    private String imageUrl;

    private BigDecimal unitPrice;

    private Integer quantity;

    private Map<String, String> variantInfo;
}
