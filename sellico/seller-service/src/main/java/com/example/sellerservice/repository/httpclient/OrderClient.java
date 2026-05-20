package com.example.sellerservice.repository.httpclient;

import com.example.sellerservice.dto.response.SellerDashboardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/internal/v1/orders/seller/{sellerId}")
    ApiResponse<Object> getSellerOrders(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "1") int size
    );
}
