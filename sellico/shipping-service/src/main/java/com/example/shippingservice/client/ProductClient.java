package com.example.shippingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductClient {
    @PostMapping("/internal/v1/skus/batch")
    ApiResponse<List<SkuDetailResponse>> getBatchSkus(@RequestBody BatchSkusRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SkuDetailResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private UUID sellerId;
        private String skuCode;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private String status;
        private String imageUrl;
        private Integer weightGram;
        private Integer lengthCm;
        private Integer widthCm;
        private Integer heightCm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class BatchSkusRequest {
        private List<UUID> skuIds;
        private List<String> skuCodes;
    }
}
