package com.example.orderservice.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Producer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send message to topic {}: {}", topic, ex.getMessage());
                throw new RuntimeException("Failed to send message to Kafka", ex);
            } else {
                log.debug("Message sent to topic {} with offset {}",
                        topic, result.getRecordMetadata().offset());
            }
        });
    }

    public void sendSync(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get();
            log.debug("Message sent synchronously to topic {}", topic);
        } catch (Exception e) {
            log.error("Failed to send message synchronously to topic {}", topic, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}

