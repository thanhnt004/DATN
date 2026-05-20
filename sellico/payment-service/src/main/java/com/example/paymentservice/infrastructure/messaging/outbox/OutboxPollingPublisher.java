package com.example.paymentservice.infrastructure.messaging.outbox;

import com.example.paymentservice.adapter.out.persistence.entity.OutboxEventJpaEntity;
import com.example.paymentservice.adapter.out.persistence.repository.OutboxJpaRepository;
import com.example.paymentservice.infrastructure.messaging.kafka.KafkaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Polls outbox table and publishes pending events to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxJpaRepository outboxRepo;
    private final KafkaEventProducer kafkaProducer;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> pending = outboxRepo.findPendingEventsWithLimit(BATCH_SIZE);
        if (pending.isEmpty()) return;

        for (OutboxEventJpaEntity event : pending) {
            try {
                String topic = resolveTopicName(event.getEventType());
                kafkaProducer.send(topic, event.getAggregateId().toString(), event.getPayload());
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxRepo.save(event);
                log.debug("Published event {} to topic {}", event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                }
                outboxRepo.save(event);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxRepo.deletePublishedEventsBefore(cutoff);
        log.info("Cleaned up published outbox events older than 7 days");
    }

    private String resolveTopicName(String eventType) {
        return switch (eventType) {
            case "PaymentCompletedEvent" -> "payment.completed";
            case "PaymentFailedEvent" -> "payment.failed";
            case "PaymentExpiredEvent" -> "payment.expired";
            default -> "payment.events";
        };
    }
}

