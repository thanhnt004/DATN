package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryLog;
import com.example.inventoryservice.entity.enums.InventoryLogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {

    /**
     * Find logs by SKU ID ordered by created date desc
     */
    List<InventoryLog> findAllByInventory_SkuIdOrderByCreatedAtDesc(UUID skuId);

    /**
     * Find logs by SKU ID with pagination
     */
    Page<InventoryLog> findAllByInventory_SkuId(UUID skuId, Pageable pageable);

    /**
     * Find logs by SKU ID and type
     */
    List<InventoryLog> findAllByInventory_SkuIdAndType(UUID skuId, InventoryLogType type);

    /**
     * Find logs by reference ID
     */
    List<InventoryLog> findAllByReferenceId(UUID referenceId);

    /**
     * Find logs by SKU ID within date range
     */
    @Query("SELECT l FROM InventoryLog l WHERE l.inventory.skuId = :skuId " +
           "AND l.createdAt BETWEEN :startDate AND :endDate ORDER BY l.createdAt DESC")
    List<InventoryLog> findBySkuIdAndDateRange(
            @Param("skuId") UUID skuId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Calculate total stock change for a SKU within date range
     */
    @Query("SELECT COALESCE(SUM(l.changeAmount), 0) FROM InventoryLog l " +
           "WHERE l.inventory.skuId = :skuId AND l.createdAt BETWEEN :startDate AND :endDate")
    Integer calculateTotalChange(
            @Param("skuId") UUID skuId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}

