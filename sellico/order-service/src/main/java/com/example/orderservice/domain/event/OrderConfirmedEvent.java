package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Event published when an order is confirmed by seller
 */
@Getter
public class OrderConfirmedEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final UUID sellerId;

    @Builder
    public OrderConfirmedEvent(UUID orderId, String orderNumber, UUID userId, UUID sellerId) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.sellerId = sellerId;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }
}

