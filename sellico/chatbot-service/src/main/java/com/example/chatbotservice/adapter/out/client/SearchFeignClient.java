package com.example.chatbotservice.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import response.ApiResponse;

import java.util.Map;

@FeignClient(name = "search-service")
public interface SearchFeignClient {

    @GetMapping("/api/v1/search/products")
    ApiResponse<Map<String, Object>> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "6") int size,
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(value = "sortBy", defaultValue = "relevance") String sortBy
    );
}
