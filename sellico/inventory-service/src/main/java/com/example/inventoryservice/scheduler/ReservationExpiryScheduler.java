package com.example.inventoryservice.scheduler;

import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private final InventoryService inventoryService;

    /**
     * Process expired reservations every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void processExpiredReservations() {
        try {
            int count = inventoryService.processExpiredReservations();
            if (count > 0) {
                log.info("Processed {} expired reservations", count);
            }
        } catch (Exception e) {
            log.error("Error processing expired reservations", e);
        }
    }
}

