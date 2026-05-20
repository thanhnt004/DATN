package com.example.inventoryservice.entity;

import com.example.inventoryservice.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "sku_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Inventory inventory;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Check if reservation is expired
     */
    public boolean isExpired() {
        return status == ReservationStatus.PENDING && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if reservation can be confirmed
     */
    public boolean canConfirm() {
        return status == ReservationStatus.PENDING && !isExpired();
    }

    /**
     * Check if reservation can be cancelled
     */
    public boolean canCancel() {
        return status == ReservationStatus.PENDING;
    }

    /**
     * Confirm the reservation
     */
    public void confirm() {
        if (!canConfirm()) {
            throw new IllegalStateException("Reservation cannot be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Cancel the reservation
     */
    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Reservation cannot be cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Mark as expired
     */
    public void markExpired() {
        if (status == ReservationStatus.PENDING) {
            this.status = ReservationStatus.EXPIRED;
        }
    }
}

