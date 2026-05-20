package com.example.orderservice.infrastructure.persistence.entity;

import com.example.orderservice.domain.model.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_saga_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "inventory_reserved")
    private Boolean inventoryReserved;

    @Column(name = "payment_processed")
    private Boolean paymentProcessed;

    @Column(name = "cart_cleared")
    private Boolean cartCleared;

    @Column(name = "notification_sent")
    private Boolean notificationSent;

    @Column(name = "inventory_released")
    private Boolean inventoryReleased;

    @Column(name = "payment_refunded")
    private Boolean paymentRefunded;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

