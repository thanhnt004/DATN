package com.example.searchservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "category-service")
public interface CategoryFeignClient {

    /**
     * Returns the given category ID plus all its descendant IDs.
     */
    @GetMapping("/internal/v1/categories/{id}/descendant-ids")
    List<String> getDescendantIds(@PathVariable("id") String id);
}
