package com.example.productservice.application.port.in;

import com.example.productservice.domain.model.Product;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetSellerProductsUseCase {
    /**
     * List products owned by a specific seller (includes all statuses except DELETED)
     * Used by seller dashboard to manage their products
     */
    Page<Product> getSellerProducts(UUID sellerId, String status, String keyword, int page, int size, String sortBy, String sortDirection);
}

