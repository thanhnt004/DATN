package com.example.productservice.application.service;

import com.example.productservice.application.port.in.GetBatchSkusUseCase;
import com.example.productservice.application.port.out.SkuRepositoryPort;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetBatchSkusService implements GetBatchSkusUseCase {

    private final SkuRepositoryPort skuRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getBatchSkusByIds(List<UUID> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return skuRepositoryPort.findAllByIds(skuIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> getBatchSkusByCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        return skuRepositoryPort.findAllByCodes(skuCodes);
    }
}

