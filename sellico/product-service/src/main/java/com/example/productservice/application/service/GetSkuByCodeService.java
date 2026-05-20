package com.example.productservice.application.service;

import com.example.productservice.application.port.in.GetSkuByCodeUseCase;
import com.example.productservice.application.port.out.SkuRepositoryPort;
import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.Product;
import com.example.productservice.domain.model.ProductSku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSkuByCodeService implements GetSkuByCodeUseCase {

    private final SkuRepositoryPort skuRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public ProductSku getSkuByCode(String skuCode) {
        return skuRepositoryPort.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.SKU_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public SkuWithProduct getSkuWithProductByCode(String skuCode) {
        ProductSku sku = skuRepositoryPort.findBySkuCode(skuCode)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.SKU_NOT_FOUND));

        Product product = skuRepositoryPort.findProductBySkuCode(skuCode)
                .orElseThrow(() -> new ProductBusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        return new SkuWithProduct(sku, product.getId(), product.getName(), product.getSellerId());
    }
}
