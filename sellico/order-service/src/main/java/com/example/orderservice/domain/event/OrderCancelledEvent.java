package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Event published when an order is cancelled
 */
@Getter
public class OrderCancelledEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final String reason;
    private final boolean inventoryReserved;
    private final boolean paymentProcessed;

    @Builder
    public OrderCancelledEvent(UUID orderId, String orderNumber, UUID userId, String reason,
                                boolean inventoryReserved, boolean paymentProcessed) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.reason = reason;
        this.inventoryReserved = inventoryReserved;
        this.paymentProcessed = paymentProcessed;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }
}

