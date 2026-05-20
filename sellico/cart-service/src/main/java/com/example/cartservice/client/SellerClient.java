package com.example.cartservice.client;

import com.example.cartservice.client.dto.SellerInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "seller-service", fallback = SellerClientFallback.class)
public interface SellerClient {

    @GetMapping("/internal/v1/sellers/batch")
    ApiResponse<List<SellerInfo>> getBatchSellers(@RequestParam("ids") List<UUID> ids);
}
