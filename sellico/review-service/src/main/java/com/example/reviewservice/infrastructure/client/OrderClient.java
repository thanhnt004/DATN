package com.example.reviewservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/internal/v1/orders/{orderId}/belongs-to/{userId}")
    ApiResponse<Boolean> checkOrderBelongsToUser(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("userId") UUID userId
    );

    @GetMapping("/internal/v1/orders/{orderId}/has-product/{productId}")
    ApiResponse<Boolean> checkOrderHasProduct(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("productId") UUID productId
    );

    @GetMapping("/internal/v1/orders/{orderId}/is-completed")
    ApiResponse<Boolean> checkOrderIsCompleted(
            @PathVariable("orderId") UUID orderId
    );
}
