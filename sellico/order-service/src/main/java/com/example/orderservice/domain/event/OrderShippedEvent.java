package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Event published when an order is shipped
 */
@Getter
public class OrderShippedEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final String shippingProvider;
    private final String trackingNumber;

    @Builder
    public OrderShippedEvent(UUID orderId, String orderNumber, UUID userId,
                              String shippingProvider, String trackingNumber) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.shippingProvider = shippingProvider;
        this.trackingNumber = trackingNumber;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }
}

