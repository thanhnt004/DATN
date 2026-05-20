package com.example.cartservice.client;

import com.example.cartservice.client.dto.SellerInfo;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

@Component
public class SellerClientFallback implements SellerClient {

    @Override
    public ApiResponse<List<SellerInfo>> getBatchSellers(List<UUID> ids) {
        return null;
    }
}
