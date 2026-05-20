package com.example.productservice.application.port.out;

import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepositoryPort {
    Optional<ProductSku> findById(UUID skuId);
    Optional<ProductSku> findBySkuCode(String skuCode);
    Optional<Product> findProductBySkuId(UUID skuId);
    Optional<Product> findProductBySkuCode(String skuCode);
    List<ProductSku> findAllByIds(List<UUID> skuIds);
    List<ProductSku> findAllByCodes(List<String> skuCodes);
    ProductSku save(ProductSku sku);
    void deleteById(UUID skuId);
}
