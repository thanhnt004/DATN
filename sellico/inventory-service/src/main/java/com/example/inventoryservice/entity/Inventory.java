package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @Column(name = "sku_id")
    private UUID skuId;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    @Formula("total_stock - reserved_stock")
    private Integer availableStock;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    @Column(name = "location_code", length = 50)
    private String locationCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryReservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    @Builder.Default
    private List<InventoryLog> logs = new ArrayList<>();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Calculate available stock manually (for cases where @Formula doesn't work)
     */
    public Integer getAvailableStock() {
        return totalStock - reservedStock;
    }

    /**
     * Check if stock is below threshold
     */
    public boolean isLowStock() {
        return getAvailableStock() <= lowStockThreshold;
    }

    /**
     * Check if can reserve quantity
     */
    public boolean canReserve(int quantity) {
        return getAvailableStock() >= quantity;
    }

    /**
     * Reserve stock
     */
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Not enough available stock to reserve");
        }
        this.reservedStock += quantity;
    }

    /**
     * Release reserved stock
     */
    public void releaseReservation(int quantity) {
        this.reservedStock = Math.max(0, this.reservedStock - quantity);
    }

    /**
     * Confirm reservation (deduct from total stock)
     */
    public void confirmReservation(int quantity) {
        this.totalStock -= quantity;
        this.reservedStock -= quantity;
    }

    /**
     * Add stock (restock)
     */
    public void addStock(int quantity) {
        this.totalStock += quantity;
    }

    /**
     * Return stock
     */
    public void returnStock(int quantity) {
        this.totalStock += quantity;
    }
}

