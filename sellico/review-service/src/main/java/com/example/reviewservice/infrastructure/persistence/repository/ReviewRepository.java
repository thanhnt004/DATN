package com.example.reviewservice.infrastructure.persistence.repository;

import com.example.reviewservice.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    Page<ReviewEntity> findByProductId(UUID productId, Pageable pageable);

    Page<ReviewEntity> findByProductIdAndRating(UUID productId, Integer rating, Pageable pageable);

    @Query("SELECT r FROM ReviewEntity r WHERE r.productId = :productId AND r.comment IS NOT NULL AND r.comment <> ''")
    Page<ReviewEntity> findByProductIdWithComment(@Param("productId") UUID productId, Pageable pageable);

    @Query(value = "SELECT * FROM reviews r WHERE r.product_id = :productId AND r.images IS NOT NULL AND array_length(r.images, 1) > 0",
           countQuery = "SELECT COUNT(*) FROM reviews r WHERE r.product_id = :productId AND r.images IS NOT NULL AND array_length(r.images, 1) > 0",
           nativeQuery = true)
    Page<ReviewEntity> findByProductIdWithImages(@Param("productId") UUID productId, Pageable pageable);

    Page<ReviewEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<ReviewEntity> findByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);

    boolean existsByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);

    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId GROUP BY r.rating")
    List<Object[]> countByProductIdGroupByRating(@Param("productId") UUID productId);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS double)), 0) FROM ReviewEntity r WHERE r.productId = :productId")
    Double avgRatingByProductId(@Param("productId") UUID productId);

    long countByProductId(UUID productId);

    List<ReviewEntity> findByOrderId(UUID orderId);
}
