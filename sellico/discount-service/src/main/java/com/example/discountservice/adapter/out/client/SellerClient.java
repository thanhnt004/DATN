package com.example.discountservice.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerClient {

    @GetMapping("/internal/v1/sellers/user/{userId}")
    ApiResponse<SellerResponse> getSellerByUserId(@PathVariable("userId") UUID userId);
}
