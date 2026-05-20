package com.example.productservice.application.service;

import com.example.productservice.application.command.GetBatchProductsCommand;
import com.example.productservice.application.port.in.GetBatchProductsUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBatchProductsService implements GetBatchProductsUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Product> getBatchProducts(GetBatchProductsCommand command) {
        if (command.getProductIds() == null || command.getProductIds().isEmpty()) {
            return List.of();
        }

        return productRepositoryPort.findAllByIds(command.getProductIds());
    }
}

