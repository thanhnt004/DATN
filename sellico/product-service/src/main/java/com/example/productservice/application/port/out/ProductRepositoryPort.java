package com.example.productservice.application.port.out;

import com.example.productservice.domain.model.Product;
import model.SpecAttribute;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.*;

public interface ProductRepositoryPort {
    boolean existsBySellerIdAndSlug(UUID sellerId, String slug);

    Set<String> findExistingSkuCodesBySellerId(UUID sellerId, Collection<String> skuCodes);

    Product save(Product product);

    /**
     * Update only the product's own fields (status, name, etc.) without touching child entities
     * (SKUs, options, images). Use this for lightweight updates like status changes.
     */
    Product updateProductOnly(Product product);

    /**
     * Update only the product specifications (JSONB) without touching child entities.
     */
    Product updateSpecifications(UUID productId, java.util.List<SpecAttribute> specifications);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySlug(String slug);

    void deleteById(UUID id);

    Page<Product> findAllWithFilters(
            UUID categoryId,
            UUID sellerId,
            String status,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    Page<Product> findBySellerIdWithFilters(
            UUID sellerId,
            String status,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    List<Product> findByCategoryIdAndStatus(UUID categoryId, String status, int limit, UUID excludeProductId);

    List<Product> findAllByIds(List<UUID> ids);
}
