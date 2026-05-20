package com.example.orderservice.infrastructure.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShippingFeignClientFallback implements ShippingFeignClient{
    @Override
    public GHNFeeResponse calculateFee(ShippingFeeRequest request) {
        log.error("Failed to calculate shipping fee for request: {}", request);
        return GHNFeeResponse.builder()
                .total(30000)
                .build();
    }
}
