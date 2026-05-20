package com.example.orderservice.domain.event;

import com.example.orderservice.domain.model.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Event published when an order is created
 */
@Getter
public class OrderCreatedEvent extends DomainEvent {
    private final UUID orderId;
    private final String orderNumber;
    private final UUID userId;
    private final UUID sellerId;
    private final BigDecimal totalAmount;
    private final PaymentMethod paymentMethod;
    private final List<OrderItemData> items;

    @Builder
    public OrderCreatedEvent(UUID orderId, String orderNumber, UUID userId, UUID sellerId,
                              BigDecimal totalAmount, PaymentMethod paymentMethod, List<OrderItemData> items) {
        super();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.sellerId = sellerId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.items = items;
    }

    @Override
    public UUID getAggregateId() {
        return orderId;
    }

    @Getter
    @Builder
    public static class OrderItemData {
        private UUID skuId;
        private Integer quantity;
    }
}

