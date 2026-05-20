package com.example.productservice.adapter.out.client;

import com.example.productservice.application.port.out.CategoryClientPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;


@FeignClient(name = "category-service")
public interface CategoryClient {
    @GetMapping("/internal/v1/categories/{id}/exists")
    boolean exists(@PathVariable("id") UUID id);

    @GetMapping("/internal/v1/categories/{id}/is-leaf")
    boolean isLeaf(@PathVariable("id") UUID id);
}
