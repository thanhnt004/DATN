package com.example.cartservice.client;

import com.example.cartservice.client.dto.SkuInfo;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ApiResponse<SkuInfo> getSkuByCode(String skuCode) {
        return null;
    }

    @Override
    public ApiResponse<List<SkuInfo>> getBatchSkusByIds(List<UUID> ids) {
        return null;
    }
}

