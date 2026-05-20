package com.example.productservice.adapter.out.persistence;

import com.example.productservice.adapter.out.persistence.entity.ProductSkuEntity;
import com.example.productservice.adapter.out.persistence.mapper.ProductMapper;
import com.example.productservice.adapter.out.persistence.mapper.ProductSkuMapper;
import com.example.productservice.adapter.out.persistence.repository.ProductSkuJpaRepository;
import com.example.productservice.application.port.out.SkuRepositoryPort;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SkuJpaAdapter implements SkuRepositoryPort {

    private final ProductSkuJpaRepository skuRepository;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSku> findById(UUID skuId) {
        return skuRepository.findById(skuId)
                .map(skuMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSku> findBySkuCode(String skuCode) {
        return skuRepository.findBySkuCode(skuCode)
                .map(skuMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findProductBySkuId(UUID skuId) {
        return skuRepository.findProductEntityBySkuId(skuId)
                .map(productMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findProductBySkuCode(String skuCode) {
        return skuRepository.findProductEntityBySkuCode(skuCode)
                .map(productMapper::toDomain);
    }

    @Override
    @Transactional
    public ProductSku save(ProductSku sku) {
        // Load the managed entity so we only update fields, not replace relationships
        ProductSkuEntity entity = skuRepository.findById(sku.getId())
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku.getId()));

        // Update only the mutable fields
        if (sku.getPrice() != null) entity.setPrice(sku.getPrice());
        if (sku.getOriginalPrice() != null) entity.setOriginalPrice(sku.getOriginalPrice());
        if (sku.getCostPrice() != null) entity.setCostPrice(sku.getCostPrice());
        if (sku.getWeightGram() != null) entity.setWeightGram(sku.getWeightGram());
        if (sku.getLengthCm() != null) entity.setLengthCm(sku.getLengthCm());
        if (sku.getWidthCm() != null) entity.setWidthCm(sku.getWidthCm());
        if (sku.getHeightCm() != null) entity.setHeightCm(sku.getHeightCm());
        if (sku.getStatus() != null) entity.setStatus(sku.getStatus());

        ProductSkuEntity saved = skuRepository.save(entity);
        return skuMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(UUID skuId) {
        skuRepository.deleteById(skuId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> findAllByIds(List<UUID> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return skuRepository.findAllByIdWithProduct(skuIds).stream()
                .map(skuMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSku> findAllByCodes(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }
        return skuRepository.findAllBySkuCodeIn(skuCodes).stream()
                .map(skuMapper::toDomain)
                .toList();
    }
}
