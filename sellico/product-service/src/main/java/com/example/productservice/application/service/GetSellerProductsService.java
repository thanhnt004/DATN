package com.example.productservice.application.service;

import com.example.productservice.application.port.in.GetSellerProductsUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSellerProductsService implements GetSellerProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getSellerProducts(UUID sellerId, String status, String keyword,
                                           int page, int size, String sortBy, String sortDirection) {
        return productRepositoryPort.findBySellerIdWithFilters(
                sellerId, status, keyword, page, size,
                sortBy != null ? sortBy : "createdAt",
                sortDirection != null ? sortDirection : "desc"
        );
    }
}

