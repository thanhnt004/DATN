package com.example.orderservice.domain.model.enums;

public enum SagaStatus {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}

