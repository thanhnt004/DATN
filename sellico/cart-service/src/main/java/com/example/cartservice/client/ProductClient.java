package com.example.cartservice.client;

import com.example.cartservice.client.dto.SkuInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/internal/v1/skus/{skuCode}")
    ApiResponse<SkuInfo> getSkuByCode(@PathVariable("skuCode") String skuCode);

    @GetMapping("/internal/v1/skus/batch")
    ApiResponse<List<SkuInfo>> getBatchSkusByIds(@RequestParam("ids") List<UUID> ids);
}

