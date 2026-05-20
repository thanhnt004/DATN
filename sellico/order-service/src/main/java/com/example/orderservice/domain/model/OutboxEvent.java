package com.example.orderservice.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Event - for reliable event publishing
 */
@Getter
@Setter
@Builder
public class OutboxEvent {
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private Instant createdAt;
    private Instant publishedAt;
    private int retryCount;
    private String lastError;
    private Long sequenceNumber;

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    public static OutboxEvent create(String aggregateType, UUID aggregateId,
                                      String eventType, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(Instant.now())
                .retryCount(0)
                .build();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = error;
        this.retryCount++;
    }

    public void retry() {
        this.status = OutboxStatus.PENDING;
        this.retryCount++;
    }
}

