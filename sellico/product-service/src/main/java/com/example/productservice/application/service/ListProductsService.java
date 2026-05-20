package com.example.productservice.application.service;

import com.example.productservice.application.command.ListProductsCommand;
import com.example.productservice.application.port.in.ListProductsUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProductsService implements ListProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public Page<Product> listProducts(ListProductsCommand command) {
        return productRepositoryPort.findAllWithFilters(
                command.getCategoryId(),
                command.getSellerId(),
                command.getStatus(),
                command.getKeyword(),
                command.getMinPrice(),
                command.getMaxPrice(),
                command.getMinRating(),
                command.getPageOrDefault(),
                command.getSizeOrDefault(),
                command.getSortByOrDefault(),
                command.getSortDirectionOrDefault()
        );
    }
}

