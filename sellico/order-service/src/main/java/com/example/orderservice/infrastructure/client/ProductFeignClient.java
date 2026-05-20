package com.example.orderservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductFeignClient {
    @PostMapping("/internal/v1/skus/batch")
    ApiResponse<List<SkuDetailResponse>> getBatchSkus(@RequestBody BatchSkusRequest request);

    @GetMapping("/internal/v1/products/batch")
    ApiResponse<List<ProductSummaryResponse>> getBatchProducts(@org.springframework.web.bind.annotation.RequestParam("ids") List<UUID> ids);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class ProductSummaryResponse {
        private UUID id;
        private UUID categoryId;
        private String name;
    }

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

        /** Variant attributes, e.g. {"Color": "Red", "Size": "XL"} */
        @Builder.Default
        private Map<String, String> attributes = new LinkedHashMap<>();
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class BatchSkusRequest {

        private List<UUID> skuIds;

        private List<String> skuCodes;
    }
}
