package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOptionEntity, UUID> {

    // ── Template queries (product IS NULL) ──────────────────────────

    /** Admin-created global templates */
    @Query("SELECT o FROM ProductOptionEntity o WHERE o.product IS NULL AND o.source = :source ORDER BY o.name ASC")
    List<ProductOptionEntity> findTemplatesBySource(@Param("source") String source);

    /** Seller's own templates */
    @Query("SELECT o FROM ProductOptionEntity o WHERE o.product IS NULL AND o.sellerId = :sellerId ORDER BY o.name ASC")
    List<ProductOptionEntity> findTemplatesBySellerId(@Param("sellerId") UUID sellerId);

    /** All templates available to a seller = ADMIN global + seller's own */
    @Query("SELECT o FROM ProductOptionEntity o WHERE o.product IS NULL AND (o.source = 'ADMIN' OR o.sellerId = :sellerId) ORDER BY o.source ASC, o.name ASC")
    List<ProductOptionEntity> findAvailableTemplatesForSeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM ProductOptionEntity o WHERE o.product IS NULL AND LOWER(o.name) = LOWER(:name) AND o.sellerId IS NULL")
    boolean existsTemplateByNameIgnoreCaseAndSellerIdIsNull(@Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM ProductOptionEntity o WHERE o.product IS NULL AND LOWER(o.name) = LOWER(:name) AND o.sellerId = :sellerId")
    boolean existsTemplateByNameIgnoreCaseAndSellerId(@Param("name") String name, @Param("sellerId") UUID sellerId);
}
