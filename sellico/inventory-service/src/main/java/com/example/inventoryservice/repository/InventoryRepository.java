package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Find inventory by SKU ID with pessimistic lock for concurrent updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.skuId = :skuId")
    Optional<Inventory> findBySkuIdWithLock(@Param("skuId") UUID skuId);

    /**
     * Find inventories by multiple SKU IDs
     */
    @Query("SELECT i FROM Inventory i WHERE i.skuId IN :skuIds")
    List<Inventory> findAllBySkuIds(@Param("skuIds") List<UUID> skuIds);

    /**
     * Find inventories by multiple SKU IDs with lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.skuId IN :skuIds")
    List<Inventory> findAllBySkuIdsWithLock(@Param("skuIds") List<UUID> skuIds);

    /**
     * Find low stock inventories
     */
    @Query("SELECT i FROM Inventory i WHERE (i.totalStock - i.reservedStock) <= i.lowStockThreshold")
    List<Inventory> findLowStockInventories();

    /**
     * Find by location code
     */
    List<Inventory> findByLocationCode(String locationCode);

    /**
     * Check if inventory exists for SKU
     */
    boolean existsBySkuId(UUID skuId);
}

