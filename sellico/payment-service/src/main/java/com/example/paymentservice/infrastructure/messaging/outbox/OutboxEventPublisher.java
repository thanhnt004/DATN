package com.example.paymentservice.infrastructure.messaging.outbox;

import com.example.paymentservice.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.example.paymentservice.adapter.out.persistence.repository.OutboxJpaRepository;
import com.example.paymentservice.application.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Saves events to outbox table for reliable Kafka publishing.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements EventPublisherPort {

    private final OutboxJpaRepository outboxRepo;

    @Override
    public void publishPaymentCompleted(String aggregateId, String payload) {
        OutboxEventJpaEntity event = OutboxEventJpaEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.fromString(aggregateId))
                .eventType("PaymentCompletedEvent")
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
        outboxRepo.save(event);
    }
}

