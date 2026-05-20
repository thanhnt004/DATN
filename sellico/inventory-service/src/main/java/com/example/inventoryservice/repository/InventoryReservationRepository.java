package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    /**
     * Find reservation by order ID and SKU ID
     */
    Optional<InventoryReservation> findByOrderIdAndInventory_SkuId(UUID orderId, UUID skuId);

    /**
     * Find all reservations for an order
     */
    List<InventoryReservation> findAllByOrderId(UUID orderId);

    /**
     * Find all reservations for a SKU
     */
    List<InventoryReservation> findAllByInventory_SkuId(UUID skuId);

    /**
     * Find all pending reservations for a SKU
     */
    List<InventoryReservation> findAllByInventory_SkuIdAndStatus(UUID skuId, ReservationStatus status);

    /**
     * Find expired pending reservations
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") Instant now);

    /**
     * Find pending reservations expiring before a certain time
     */
    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'PENDING' AND r.expiresAt < :expiryTime")
    List<InventoryReservation> findPendingReservationsExpiringBefore(@Param("expiryTime") Instant expiryTime);

    /**
     * Count pending reservations for a SKU
     */
    long countByInventory_SkuIdAndStatus(UUID skuId, ReservationStatus status);

    /**
     * Bulk update expired reservations
     */
    @Modifying
    @Query("UPDATE InventoryReservation r SET r.status = 'EXPIRED' WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    int updateExpiredReservations(@Param("now") Instant now);

    /**
     * Check if reservation exists for order and SKU
     */
    boolean existsByOrderIdAndInventory_SkuId(UUID orderId, UUID skuId);
}

