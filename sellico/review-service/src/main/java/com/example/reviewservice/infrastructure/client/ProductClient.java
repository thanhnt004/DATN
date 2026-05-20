package com.example.reviewservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductClient {

    @PutMapping("/internal/v1/products/{id}/ratings")
    ApiResponse<Object> updateProductRating(
            @PathVariable("id") UUID productId,
            @RequestBody UpdateProductRatingRequest request
    );

    record UpdateProductRatingRequest(BigDecimal ratingAvg, Integer ratingCount) {}
}
