package com.example.chatbotservice.adapter.out.client;

import com.example.chatbotservice.application.port.out.ProductClientPort;
import com.example.chatbotservice.domain.model.ProductInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClientAdapter implements ProductClientPort {

    private final SearchFeignClient searchFeignClient;
    private final ProductFeignClient productFeignClient;

    @Override
    public List<ProductInfo> searchProducts(String keyword, int page, int size) {
        try {
            ApiResponse<Map<String, Object>> response = searchFeignClient.searchProducts(
                    keyword, page, size, "ACTIVE", "relevance"
            );
            return extractProducts(response);
        } catch (Exception e) {
            log.warn("Failed to search products via search-service for keyword '{}', falling back to product-service", keyword, e);
            // Fallback: try product-service directly
            try {
                ApiResponse<Map<String, Object>> fallbackResponse = productFeignClient.getProducts(
                        page, size, "soldCount", "desc", "ACTIVE"
                );
                return extractProductsFromProductService(fallbackResponse);
            } catch (Exception ex) {
                log.error("Fallback to product-service also failed", ex);
                return Collections.emptyList();
            }
        }
    }

    @Override
    public List<ProductInfo> getTopProducts(int limit) {
        try {
            ApiResponse<Map<String, Object>> response = productFeignClient.getProducts(
                    0, limit, "soldCount", "desc", "ACTIVE"
            );
            return extractProductsFromProductService(response);
        } catch (Exception e) {
            log.error("Failed to get top products", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProductInfo> getProductsByCategory(String categorySlug, int page, int size) {
        try {
            ApiResponse<Map<String, Object>> response = productFeignClient.getProductsByCategory(
                    categorySlug, page, size, "ACTIVE"
            );
            return extractProductsFromProductService(response);
        } catch (Exception e) {
            log.error("Failed to get products by category: {}", categorySlug, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProductInfo> extractProducts(ApiResponse<Map<String, Object>> response) {
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> data = response.getData();
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        if (content == null) {
            return Collections.emptyList();
        }

        return content.stream()
                .map(this::mapSearchResultToProductInfo)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<ProductInfo> extractProductsFromProductService(ApiResponse<Map<String, Object>> response) {
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> data = response.getData();
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        if (content == null) {
            return Collections.emptyList();
        }

        return content.stream()
                .map(this::mapProductResultToProductInfo)
                .toList();
    }

    private ProductInfo mapSearchResultToProductInfo(Map<String, Object> item) {
        return ProductInfo.builder()
                .id(parseUUID(item.get("id")))
                .name(getString(item, "name"))
                .slug(getString(item, "slug"))
                .price(parseBigDecimal(item.get("minPrice")))
                .originalPrice(parseBigDecimal(item.get("maxPrice")))
                .imageUrl(getString(item, "thumbnailUrl"))
                .ratingAvg(parseBigDecimal(item.get("ratingAvg")))
                .ratingCount(parseInteger(item.get("ratingCount")))
                .soldCount(parseInteger(item.get("soldCount")))
                .build();
    }

    private ProductInfo mapProductResultToProductInfo(Map<String, Object> item) {
        return ProductInfo.builder()
                .id(parseUUID(item.get("id")))
                .name(getString(item, "name"))
                .slug(getString(item, "slug"))
                .price(parseBigDecimal(item.get("minPrice")))
                .originalPrice(parseBigDecimal(item.get("maxPrice")))
                .imageUrl(getString(item, "primaryImageUrl"))
                .ratingAvg(parseBigDecimal(item.get("ratingAvg")))
                .ratingCount(parseInteger(item.get("ratingCount")))
                .soldCount(parseInteger(item.get("soldCount")))
                .build();
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
