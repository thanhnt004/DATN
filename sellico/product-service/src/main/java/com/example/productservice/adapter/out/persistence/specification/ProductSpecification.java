package com.example.productservice.adapter.out.persistence.specification;

import com.example.productservice.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Type-safe JPA Specifications for ProductEntity.
 * Replaces the JPQL "IS NULL OR" pattern which is broken in Hibernate 6 with UUID params.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<ProductEntity> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<ProductEntity> hasCategoryId(UUID categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<ProductEntity> hasSellerId(UUID sellerId) {
        if (sellerId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    public static Specification<ProductEntity> hasStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ProductEntity> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<ProductEntity> minPriceGte(BigDecimal minPrice) {
        if (minPrice == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("minPrice"), minPrice);
    }

    public static Specification<ProductEntity> maxPriceLte(BigDecimal maxPrice) {
        if (maxPrice == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxPrice"), maxPrice);
    }

    public static Specification<ProductEntity> ratingAvgGte(BigDecimal minRating) {
        if (minRating == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ratingAvg"), minRating);
    }

    /**
     * Build a combined specification from all filter parameters.
     * Null parameters are simply ignored (no filter applied).
     */
    public static Specification<ProductEntity> withFilters(
            UUID categoryId,
            UUID sellerId,
            String status,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating
    ) {
        return Specification
                .where(isNotDeleted())
                .and(hasCategoryId(categoryId))
                .and(hasSellerId(sellerId))
                .and(hasStatus(status))
                .and(nameContains(keyword))
                .and(minPriceGte(minPrice))
                .and(maxPriceLte(maxPrice))
                .and(ratingAvgGte(minRating));
    }
}
