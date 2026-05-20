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
public class UpdateProductRatingCommand {
    UUID productId;
    BigDecimal ratingAvg;
    Integer ratingCount;
}

