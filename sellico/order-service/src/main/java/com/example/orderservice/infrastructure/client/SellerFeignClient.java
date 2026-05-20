package com.example.orderservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import response.ApiResponse;

import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerFeignClient {

    @GetMapping("/internal/v1/sellers/user/{userId}")
    ApiResponse<SellerInfo> getSellerByUserId(@PathVariable("userId") UUID userId);

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SellerInfo {
        private UUID id;
        private String shopName;
    }
}
