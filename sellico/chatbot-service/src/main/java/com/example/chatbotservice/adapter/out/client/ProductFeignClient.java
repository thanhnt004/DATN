package com.example.chatbotservice.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/api/v1/products")
    ApiResponse<Map<String, Object>> getProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "sortBy", defaultValue = "soldCount") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status
    );

    @GetMapping("/api/v1/products")
    ApiResponse<Map<String, Object>> getProductsByCategory(
            @RequestParam(value = "categoryId") String categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status
    );
}
