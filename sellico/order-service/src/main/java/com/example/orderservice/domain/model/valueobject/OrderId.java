package com.example.orderservice.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Order ID Value Object
 */
public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

