package com.example.orderservice.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "category-service")
public interface CategoryFeignClient {

    @GetMapping("/internal/v1/categories/batch")
    List<CategoryResponse> getBatchCategories(@RequestParam("ids") List<UUID> ids);

    @PostMapping("/internal/v1/categories/batch")
    List<CategoryResponse> getBatchCategoriesByIds(@RequestBody List<UUID> ids);

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class CategoryResponse {
        private UUID id;
        private String name;
    }
}
