package com.example.orderservice.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when an order is paid
 */
@Getter
public class OrderPaidEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final UUID sellerId;
    private final BigDecimal amount;
    private final String transactionId;

    @Builder
    public OrderPaidEvent(UUID orderId, String orderNumber, UUID userId, UUID sellerId,
                          BigDecimal amount, String transactionId) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }
}

