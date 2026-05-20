package com.example.shippingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;
import com.example.shippingservice.dto.request.ShipOrderRequest;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {
    @PostMapping("/internal/v1/orders")
    ApiResponse<List<OrderResponse>> getOrderByOrderId(@RequestBody List<UUID> orderId);

    @PostMapping("/internal/v1/orders/{id}/ship")
    ApiResponse<OrderResponse> shipOrder(@PathVariable("id") UUID id, @RequestBody ShipOrderRequest request);
}
