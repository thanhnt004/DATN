package com.example.productservice.adapter.in.web.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuResponse {
    private UUID id;
    private String skuCode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private String status;
    private Integer weightGram;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
}

