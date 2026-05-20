package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Event published when an order is completed
 */
@Getter
public class OrderCompletedEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final UUID sellerId;
    private final List<OrderItemInfo> items;

    @Builder
    public OrderCompletedEvent(UUID orderId, String orderNumber, UUID userId, UUID sellerId, List<OrderItemInfo> items) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.sellerId = sellerId;
        this.items = items;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }

    @Getter
    @Builder
    public static class OrderItemInfo {
        private final UUID productId;
        private final int quantity;
    }
}

