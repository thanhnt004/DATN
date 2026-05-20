package com.example.inventoryservice.entity;

import com.example.inventoryservice.entity.enums.InventoryLogType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Inventory inventory;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InventoryLogType type;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Create a log entry for sale
     */
    public static InventoryLog sale(Inventory inventory, int quantity, UUID orderId, String note) {
        return InventoryLog.builder()
                .inventory(inventory)
                .changeAmount(-quantity)
                .type(InventoryLogType.SALE)
                .referenceId(orderId)
                .note(note)
                .build();
    }

    /**
     * Create a log entry for restock
     */
    public static InventoryLog restock(Inventory inventory, int quantity, UUID referenceId, String note) {
        return InventoryLog.builder()
                .inventory(inventory)
                .changeAmount(quantity)
                .type(InventoryLogType.RESTOCK)
                .referenceId(referenceId)
                .note(note)
                .build();
    }

    /**
     * Create a log entry for return
     */
    public static InventoryLog returnStock(Inventory inventory, int quantity, UUID orderId, String note) {
        return InventoryLog.builder()
                .inventory(inventory)
                .changeAmount(quantity)
                .type(InventoryLogType.RETURN)
                .referenceId(orderId)
                .note(note)
                .build();
    }

    /**
     * Create a log entry for adjustment
     */
    public static InventoryLog adjustment(Inventory inventory, int changeAmount, String note) {
        return InventoryLog.builder()
                .inventory(inventory)
                .changeAmount(changeAmount)
                .type(InventoryLogType.ADJUSTMENT)
                .note(note)
                .build();
    }

    /**
     * Create a log entry for expired reservation
     */
    public static InventoryLog expiredReservation(Inventory inventory, int quantity, UUID reservationId, String note) {
        return InventoryLog.builder()
                .inventory(inventory)
                .changeAmount(quantity)
                .type(InventoryLogType.EXPIRED_RESERVATION)
                .referenceId(reservationId)
                .note(note)
                .build();
    }
}

