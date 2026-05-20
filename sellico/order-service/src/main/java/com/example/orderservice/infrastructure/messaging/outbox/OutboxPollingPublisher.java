package com.example.orderservice.infrastructure.messaging.outbox;

import com.example.orderservice.domain.model.OutboxEvent.OutboxStatus;
import com.example.orderservice.infrastructure.messaging.kafka.KafkaEventProducer;
import com.example.orderservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.example.orderservice.infrastructure.persistence.repository.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Outbox Polling Publisher - polls outbox table and publishes to Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxJpaRepository outboxRepository;
    private final KafkaEventProducer kafkaProducer;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    /**
     * Poll outbox table every 5 seconds and publish pending events to Kafka
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findPendingEventsWithLimit(BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                // Determine topic based on event type
                String topic = resolveTopicName(event.getEventType());

                // Publish to Kafka
                kafkaProducer.send(topic, event.getAggregateId().toString(), event.getPayload());

                // Mark as published
                outboxRepository.updateStatus(event.getId(), OutboxStatus.PUBLISHED, Instant.now());

                log.debug("Published event: {} to topic: {}", event.getEventType(), topic);

            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                handlePublishFailure(event, e.getMessage());
            }
        }
    }

    /**
     * Clean up old published events (older than 7 days)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffTime = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxRepository.deletePublishedEventsBefore(cutoffTime);
        log.info("Cleaned up published events older than 7 days");
    }

    private String resolveTopicName(String eventType) {
        return switch (eventType) {
            case "OrderCreatedEvent" -> "order.created";
            case "OrderPaidEvent" -> "order.paid";
            case "OrderConfirmedEvent" -> "order.confirmed";
            case "OrderShippedEvent" -> "order.shipped";
            case "OrderDeliveredEvent" -> "order.delivered";
            case "OrderCompletedEvent" -> "order.completed";
            case "OrderCancelledEvent" -> "order.cancelled";
            case "PlatformCouponAppliedEvent" -> "discount.platform-coupon.applied";
            default -> "order.events";
        };
    }

    private void handlePublishFailure(OutboxEventJpaEntity event, String error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(error);

        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Event {} failed after {} retries", event.getId(), MAX_RETRIES);
        }

        outboxRepository.save(event);
    }
}

