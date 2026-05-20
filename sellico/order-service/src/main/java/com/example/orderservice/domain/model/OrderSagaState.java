package com.example.orderservice.domain.model;

import com.example.orderservice.domain.model.enums.SagaStatus;
import com.example.orderservice.domain.model.valueobject.OrderId;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga State - tracks the progress of order creation saga
 */
@Getter
@Setter
@Builder
public class OrderSagaState {
    private UUID id;
    private OrderId orderId;
    private SagaStatus status;
    private String currentStep;

    // Step completion tracking
    private boolean inventoryReserved;
    private boolean paymentProcessed;
    private boolean cartCleared;
    private boolean notificationSent;

    // Compensation tracking
    private boolean inventoryReleased;
    private boolean paymentRefunded;

    // Error tracking
    private String lastError;
    private int retryCount;
    private int maxRetries;

    private Instant createdAt;
    private Instant updatedAt;

    public static OrderSagaState create(OrderId orderId) {
        return OrderSagaState.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(SagaStatus.STARTED)
                .currentStep("RESERVE_INVENTORY")
                .maxRetries(3)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void markInventoryReserved() {
        this.inventoryReserved = true;
        this.status = SagaStatus.INVENTORY_RESERVED;
        this.currentStep = "AWAIT_PAYMENT";
        this.updatedAt = Instant.now();
    }

    public void markPaymentPending() {
        this.status = SagaStatus.PAYMENT_PENDING;
        this.currentStep = "PROCESS_PAYMENT";
        this.updatedAt = Instant.now();
    }

    public void markPaymentCompleted() {
        this.paymentProcessed = true;
        this.status = SagaStatus.PAYMENT_COMPLETED;
        this.currentStep = "CLEAR_CART";
        this.updatedAt = Instant.now();
    }

    public void markCartCleared() {
        this.cartCleared = true;
        this.currentStep = "SEND_NOTIFICATION";
        this.updatedAt = Instant.now();
    }

    public void markNotificationSent() {
        this.notificationSent = true;
        this.currentStep = "COMPLETED";
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.currentStep = "DONE";
        this.updatedAt = Instant.now();
    }

    public void startCompensation(String error) {
        this.status = SagaStatus.COMPENSATING;
        this.lastError = error;
        this.currentStep = "COMPENSATE_" + this.currentStep;
        this.updatedAt = Instant.now();
    }

    public void markInventoryReleased() {
        this.inventoryReleased = true;
        this.updatedAt = Instant.now();
    }

    public void markPaymentRefunded() {
        this.paymentRefunded = true;
        this.updatedAt = Instant.now();
    }

    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.currentStep = "COMPENSATED";
        this.updatedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = SagaStatus.FAILED;
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }
}

