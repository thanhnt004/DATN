package com.example.orderservice.infrastructure.client;

import com.example.orderservice.domain.port.output.InventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Inventory Service Adapter - implements InventoryPort
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceAdapter implements InventoryPort {

    private final InventoryFeignClient inventoryClient;

    @Override
    public boolean reserveStock(UUID orderId, List<ReservationItem> items) {
        try {
            log.info("[Inventory] Sending reserve request for order: {}, items: {}", orderId, items);

            var request = new InventoryFeignClient.ReserveStockRequest(
                    orderId,
                    items.stream()
                            .map(item -> new InventoryFeignClient.ReservationItemDto(item.skuId(), item.quantity()))
                            .toList(),
                    30 // 30 minutes reservation
            );

            var response = inventoryClient.reserveStock(request);
            log.info("[Inventory] Reserve response for order {}: success={}, data={}",
                    orderId, response != null ? response.isSuccess() : "null",
                    response != null ? response.getData() : "null");

            if (response == null) {
                log.error("[Inventory] Null response for order: {}", orderId);
                return false;
            }
            if (!response.isSuccess()) {
                log.error("[Inventory] API returned failure for order: {}, message: {}", orderId, response.getMessage());
                return false;
            }
            if (response.getData() == null) {
                log.error("[Inventory] Response data is null for order: {}", orderId);
                return false;
            }

            boolean reserved = response.getData().getSuccess() != null && response.getData().getSuccess();
            log.info("[Inventory] Reserve result for order {}: reserved={}, failedItems={}",
                    orderId, reserved, response.getData().getFailedItems());
            return reserved;

        } catch (Exception e) {
            log.error("[Inventory] Failed to reserve stock for order: {}", orderId, e);
            return false;
        }
    }

    @Override
    public void confirmReservation(UUID orderId) {
        try {
            inventoryClient.confirmReservation(orderId);
            log.info("Reservation confirmed for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to confirm reservation for order: {}", orderId, e);
        }
    }

    @Override
    public void releaseReservation(UUID orderId) {
        try {
            inventoryClient.cancelReservation(orderId);
            log.info("Reservation released for order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to release reservation for order: {}", orderId, e);
        }
    }
}

