package com.example.productservice.adapter.out.persistence.repository;

import com.example.productservice.adapter.out.persistence.entity.ProductSkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductSkuJpaRepository extends JpaRepository<ProductSkuEntity, UUID> {
    @Query("""
        SELECT ps.skuCode
        FROM ProductSkuEntity ps
        JOIN ps.product p
        WHERE p.sellerId = :sellerId
          AND ps.skuCode IN :skuCodes
    """)
    Set<String> findExistingSkuCodesBySellerId(
            @Param("sellerId") UUID sellerId,
            @Param("skuCodes") Collection<String> skuCodes
    );

    @Query("""
        SELECT ps.product
        FROM ProductSkuEntity ps
        WHERE ps.id = :skuId
    """)
    Optional<com.example.productservice.adapter.out.persistence.entity.ProductEntity> findProductEntityBySkuId(@Param("skuId") UUID skuId);

    @Query("""
        SELECT ps.product
        FROM ProductSkuEntity ps
        WHERE ps.skuCode = :skuCode
    """)
    Optional<com.example.productservice.adapter.out.persistence.entity.ProductEntity> findProductEntityBySkuCode(@Param("skuCode") String skuCode);

    Optional<ProductSkuEntity> findBySkuCode(String skuCode);

    List<ProductSkuEntity> findAllBySkuCodeIn(List<String> skuCodes);

    @Query("SELECT DISTINCT s FROM ProductSkuEntity s JOIN FETCH s.product p LEFT JOIN FETCH p.images WHERE s.id IN :ids")
    List<ProductSkuEntity> findAllByIdWithProduct(@Param("ids") List<UUID> ids);
}
