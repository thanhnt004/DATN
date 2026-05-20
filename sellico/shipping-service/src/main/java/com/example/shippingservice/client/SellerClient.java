package com.example.shippingservice.client;

import com.example.shippingservice.dto.response.SellerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerClient {

    @GetMapping("/internal/v1/sellers/{sellerId}")
    ApiResponse<SellerResponse> getSeller(@PathVariable("sellerId") UUID sellerId);
}
