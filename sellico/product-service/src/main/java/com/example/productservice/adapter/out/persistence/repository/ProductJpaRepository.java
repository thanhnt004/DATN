package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    boolean existsBySellerIdAndSlug(UUID sellerId, String slug);

    @EntityGraph(attributePaths = {"images"})
    Optional<ProductEntity> findBySlug(String slug);

    // Optimized query for list view - no EntityGraph for images (images loaded lazy or separately)
    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:sellerId IS NULL OR p.sellerId = :sellerId)
          AND (:status IS NULL OR p.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          AND (:minPrice IS NULL OR p.minPrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.maxPrice <= :maxPrice)
          AND (:minRating IS NULL OR p.ratingAvg >= :minRating)
    """)
    Page<ProductEntity> findAllWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("sellerId") UUID sellerId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable
    );

    // Optimized query for detail view with images
    @EntityGraph(attributePaths = {"images"})
    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.isDeleted = false
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:sellerId IS NULL OR p.sellerId = :sellerId)
          AND (:status IS NULL OR p.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          AND (:minPrice IS NULL OR p.minPrice >= :minPrice)
          AND (:maxPrice IS NULL OR p.maxPrice <= :maxPrice)
          AND (:minRating IS NULL OR p.ratingAvg >= :minRating)
    """)
    Page<ProductEntity> findAllWithFiltersWithImages(
            @Param("categoryId") UUID categoryId,
            @Param("sellerId") UUID sellerId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.isDeleted = false
          AND p.sellerId = :sellerId
          AND (:status IS NULL OR p.status = :status)
          AND (CAST(:keyword AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
    """)
    Page<ProductEntity> findBySellerIdWithFilters(
            @Param("sellerId") UUID sellerId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"images"})
    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.isDeleted = false
          AND p.categoryId = :categoryId
          AND p.status = :status
          AND p.id <> :excludeId
        ORDER BY p.soldCount DESC
    """)
    List<ProductEntity> findRelatedProducts(
            @Param("categoryId") UUID categoryId,
            @Param("status") String status,
            @Param("excludeId") UUID excludeId,
            Pageable pageable
    );
}
