package com.example.orderservice.infrastructure.messaging.outbox;

import com.example.orderservice.domain.event.DomainEvent;
import com.example.orderservice.domain.model.OutboxEvent;
import com.example.orderservice.domain.port.output.EventPublisher;
import com.example.orderservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.orderservice.infrastructure.persistence.repository.OutboxJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outbox Event Publisher - saves events to outbox table within same transaction
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEventJpaEntity outboxEvent = OutboxEventJpaEntity.builder()
                    .id(event.getEventId())
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .payload(payload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(event.getOccurredOn())
                    .retryCount(0)
                    .build();

            outboxRepository.save(outboxEvent);
            log.debug("Event saved to outbox: {} - {}", event.getEventType(), event.getAggregateId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}

