package com.example.productservice.domain.service;

import com.example.productservice.domain.exception.ProductBusinessException;
import com.example.productservice.domain.exception.ProductErrorCode;
import com.example.productservice.domain.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductDomainService {
    public void validateNewProduct(Product product) {
        validateBasicInfo(product);
        validateImages(product.getImages());
        validateOptions(product.getOptions());
        validateSkus(product.getOptions(), product.getSkus());
    }

    private void validateBasicInfo(Product product) {
        if (product.getSellerId() == null)
            throw new ProductBusinessException(ProductErrorCode.SELLER_ID_REQUIRED);

        if (product.getName() == null || product.getName().isBlank())
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_NAME_REQUIRED);

        if (product.getSkus() == null || product.getSkus().isEmpty())
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_MUST_HAVE_SKU);
    }
    private void validateImages(List<ProductImage> images) {
        if (images == null || images.isEmpty())
            throw new ProductBusinessException(ProductErrorCode.PRODUCT_IMAGE_REQUIRED);

        long primaryCount = images.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .count();

        if (primaryCount > 1)
            throw new ProductBusinessException(ProductErrorCode.ONLY_ONE_PRIMARY_IMAGE);

        for (ProductImage img : images) {
            if (img.getUrl() == null || img.getUrl().isBlank())
                throw new ProductBusinessException(ProductErrorCode.IMAGE_URL_REQUIRED);

            if (img.getSortOrder() != null && img.getSortOrder() < 0)
                throw new ProductBusinessException(ProductErrorCode.IMAGE_SORT_ORDER_INVALID);
        }
    }
    private void validateOptions(List<ProductOption> options) {
        if (options == null || options.isEmpty()) return;

        Set<String> optionNames = new HashSet<>();

        for (ProductOption opt : options) {
            if (opt.getName() == null || opt.getName().isBlank())
                throw new ProductBusinessException(ProductErrorCode.OPTION_NAME_REQUIRED);

            if (!optionNames.add(opt.getName()))
                throw new ProductBusinessException(ProductErrorCode.DUPLICATE_OPTION_NAME);

            if (opt.getValues() == null || opt.getValues().isEmpty())
                throw new ProductBusinessException(ProductErrorCode.OPTION_VALUE_REQUIRED);

            Set<String> values = new HashSet<>();
            for (ProductOptionValue val : opt.getValues()) {
                if (val.getValue() == null || val.getValue().isBlank())
                    throw new ProductBusinessException(ProductErrorCode.OPTION_VALUE_REQUIRED);

                if (!values.add(val.getValue()))
                    throw new ProductBusinessException(ProductErrorCode.DUPLICATE_OPTION_VALUE);
            }
        }
    }
    private void validateSkus(List<ProductOption> options, List<ProductSku> skus) {

        Set<String> skuCodes = new HashSet<>();
        Set<String> combinations = new HashSet<>();

        for (ProductSku sku : skus) {
            if (sku.getSkuCode() == null || sku.getSkuCode().isBlank())
                throw new ProductBusinessException(ProductErrorCode.SKU_CODE_REQUIRED);
            if (!skuCodes.add(sku.getSkuCode()))
                throw new ProductBusinessException(ProductErrorCode.DUPLICATE_SKU_CODE);

            if (sku.getPrice() == null || sku.getPrice().compareTo(BigDecimal.ZERO) <= 0)
                throw new ProductBusinessException(ProductErrorCode.PRICE_INVALID);

            if (sku.getOriginalPrice() != null &&
                    sku.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0 &&
                    sku.getOriginalPrice().compareTo(sku.getPrice()) < 0)
                throw new ProductBusinessException(ProductErrorCode.ORIGINAL_PRICE_INVALID);

            if (sku.getWeightGram() == null || sku.getWeightGram() <= 0)
                throw new ProductBusinessException(ProductErrorCode.WEIGHT_INVALID);

            validateSkuOptions(options, sku);

            String key = buildCombinationKey(sku.getSelectedValues());
            if (!combinations.add(key))
                throw new ProductBusinessException(ProductErrorCode.DUPLICATE_SKU_COMBINATION);
        }
    }
    private void validateSkuOptions(List<ProductOption> options, ProductSku sku) {

        if (options == null || options.isEmpty()) {
            if (sku.getSelectedValues() != null && !sku.getSelectedValues().isEmpty())
                throw new ProductBusinessException(ProductErrorCode.SKU_OPTION_MISMATCH);
            return;
        }

        if (sku.getSelectedValues() == null ||
                sku.getSelectedValues().size() != options.size())
            throw new ProductBusinessException(ProductErrorCode.SKU_OPTION_MISMATCH);

        for (ProductOption opt : options) {
            boolean matched = sku.getSelectedValues().stream()
                    .anyMatch(v -> opt.getValues().stream()
                            .anyMatch(ov -> ov.getValue().equals(v.getValue()))
                    );

            if (!matched)
                throw new ProductBusinessException(ProductErrorCode.INVALID_OPTION_VALUE);
        }
    }


    private String buildCombinationKey(List<ProductOptionValue> values) {
        return values.stream()
                .map(ProductOptionValue::getValue)
                .sorted()
                .reduce((a, b) -> a + "-" + b)
                .orElse("");
    }

}
