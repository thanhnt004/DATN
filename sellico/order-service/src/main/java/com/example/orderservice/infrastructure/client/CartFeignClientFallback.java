package com.example.orderservice.infrastructure.client;

import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.UUID;

@Component
public class CartFeignClientFallback implements CartFeignClient {

    @Override
    public ApiResponse<Void> clearSelectedItems(UUID userId) {
        return null;
    }
}

