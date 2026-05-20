package com.example.productservice.adapter.out.client;

import com.example.productservice.adapter.out.client.dto.SellerResponse;
import com.example.productservice.application.port.out.SellerClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerClientAdapter implements SellerClientPort {
    private final SellerClient sellerClient;

    @Override
    public boolean isSellerActive(UUID sellerId) {
        try {
            ApiResponse<Boolean> response = sellerClient.isSellerActive(sellerId);
            return response != null && response.getData() != null && response.getData();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UUID getSellerIdByUserId(UUID userId) {
        try {
            ApiResponse<SellerResponse> response = sellerClient.getSellerByUserId(userId);
            if (response != null && response.getData() != null) {
                return response.getData().getId();
            }
            throw new RuntimeException("Seller not found for userId: " + userId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve sellerId for userId: " + userId, e);
        }
    }
}
