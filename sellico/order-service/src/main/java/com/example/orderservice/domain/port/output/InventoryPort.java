package com.example.orderservice.domain.port.output;

import java.util.List;
import java.util.UUID;

/**
 * Output Port - Inventory Service Interface
 * Implemented by Infrastructure layer (Feign Client or Kafka)
 */
public interface InventoryPort {

    /**
     * Reserve stock for order items
     * @return true if all items reserved successfully
     */
    boolean reserveStock(UUID orderId, List<ReservationItem> items);

    /**
     * Confirm reservation (deduct stock permanently)
     */
    void confirmReservation(UUID orderId);

    /**
     * Release/cancel reservation
     */
    void releaseReservation(UUID orderId);

    record ReservationItem(UUID skuId, int quantity) {}
}

