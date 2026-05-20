package com.example.productservice.adapter.out.client;

import com.example.productservice.application.port.out.CategoryClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryClientAdapter implements CategoryClientPort {
    private final CategoryClient categoryClient;
    @Override
    public boolean isLeaf(UUID categoryId) {
        return categoryClient.isLeaf(categoryId);
    }

    @Override
    public boolean isExist(UUID categoryId) {
        return categoryClient.exists(categoryId);
    }

}
