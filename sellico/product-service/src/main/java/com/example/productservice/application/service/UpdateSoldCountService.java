package com.example.productservice.application.service;

import com.example.productservice.application.command.UpdateSoldCountCommand;
import com.example.productservice.application.port.in.UpdateSoldCountUseCase;
import com.example.productservice.application.port.out.ProductRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UpdateSoldCountService implements UpdateSoldCountUseCase {

    private final ProductRepositoryPort productRepositoryPort;

    @Override
    @Transactional
    public Product updateSoldCount(UpdateSoldCountCommand command) {
        Product product = productRepositoryPort.findById(command.getProductId())
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        int currentSold = product.getSoldCount() != null ? product.getSoldCount() : 0;
        product.setSoldCount(currentSold + command.getQuantity());
        product.setUpdatedAt(Instant.now());

        return productRepositoryPort.updateProductOnly(product);
    }
}

