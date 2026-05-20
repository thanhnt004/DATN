package com.example.productservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@Value
public class UpdateSkuCommand {
    UUID skuId;
    UUID sellerId;
    BigDecimal price;
    BigDecimal originalPrice;
    BigDecimal costPrice;
    Integer weightGram;
    Integer lengthCm;
    Integer widthCm;
    Integer heightCm;
    String status;
}

