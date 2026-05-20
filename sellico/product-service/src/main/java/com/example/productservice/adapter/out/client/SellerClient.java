package com.example.productservice.adapter.out.client;

import com.example.productservice.adapter.out.client.dto.SellerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerClient {
    @GetMapping("/internal/v1/sellers/{sellerId}/active")
    ApiResponse<Boolean> isSellerActive(@PathVariable("sellerId") UUID sellerId);

    @GetMapping("/internal/v1/sellers/user/{userId}")
    ApiResponse<SellerResponse> getSellerByUserId(@PathVariable("userId") UUID userId);
}
