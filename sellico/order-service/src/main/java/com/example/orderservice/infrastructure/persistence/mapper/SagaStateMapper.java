package com.example.orderservice.infrastructure.persistence.mapper;

import com.example.orderservice.domain.model.OrderSagaState;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.infrastructure.persistence.entity.SagaStateJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SagaStateMapper {

    public SagaStateJpaEntity toEntity(OrderSagaState saga) {
        return SagaStateJpaEntity.builder()
                .id(saga.getId())
                .orderId(saga.getOrderId().value())
                .sagaStatus(saga.getStatus())
                .currentStep(saga.getCurrentStep())
                .inventoryReserved(saga.isInventoryReserved())
                .paymentProcessed(saga.isPaymentProcessed())
                .cartCleared(saga.isCartCleared())
                .notificationSent(saga.isNotificationSent())
                .inventoryReleased(saga.isInventoryReleased())
                .paymentRefunded(saga.isPaymentRefunded())
                .lastError(saga.getLastError())
                .retryCount(saga.getRetryCount())
                .maxRetries(saga.getMaxRetries())
                .createdAt(saga.getCreatedAt())
                .updatedAt(saga.getUpdatedAt())
                .build();
    }

    public OrderSagaState toDomain(SagaStateJpaEntity entity) {
        return OrderSagaState.builder()
                .id(entity.getId())
                .orderId(new OrderId(entity.getOrderId()))
                .status(entity.getSagaStatus())
                .currentStep(entity.getCurrentStep())
                .inventoryReserved(entity.getInventoryReserved() != null && entity.getInventoryReserved())
                .paymentProcessed(entity.getPaymentProcessed() != null && entity.getPaymentProcessed())
                .cartCleared(entity.getCartCleared() != null && entity.getCartCleared())
                .notificationSent(entity.getNotificationSent() != null && entity.getNotificationSent())
                .inventoryReleased(entity.getInventoryReleased() != null && entity.getInventoryReleased())
                .paymentRefunded(entity.getPaymentRefunded() != null && entity.getPaymentRefunded())
                .lastError(entity.getLastError())
                .retryCount(entity.getRetryCount() != null ? entity.getRetryCount() : 0)
                .maxRetries(entity.getMaxRetries() != null ? entity.getMaxRetries() : 3)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

