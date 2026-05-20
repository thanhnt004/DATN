package com.example.paymentservice.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Sent to topic {} offset {}", topic, result.getRecordMetadata().offset());
            }
        });
    }
}

