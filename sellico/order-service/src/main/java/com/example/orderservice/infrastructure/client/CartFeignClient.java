package com.example.orderservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "cart-service", fallback = CartFeignClientFallback.class)
public interface CartFeignClient {

    @DeleteMapping("/internal/v1/cart/{userId}/selected")
    ApiResponse<Void> clearSelectedItems(@PathVariable("userId") UUID userId);
}

