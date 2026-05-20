package com.example.sellerservice.event;

import event.EventMetadata;
import event.EventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.seller-events:seller-events}")
    private String sellerEventsTopic;

    /**
     * Publish seller status changed event (approved, rejected, suspended, banned, reactivated).
     */
    public void publishSellerStatusChanged(String eventType, SellerStatusChangedPayload payload) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(),
                eventType,
                "seller-service",
                Instant.now()
        );

        EventWrapper<SellerStatusChangedPayload> eventWrapper = new EventWrapper<>(metadata, payload);

        kafkaTemplate.send(sellerEventsTopic, payload.getSellerId(), eventWrapper)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("{} event sent successfully! sellerId={}, offset={}",
                                eventType, payload.getSellerId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send {} event for sellerId={}: {}",
                                eventType, payload.getSellerId(), ex.getMessage());
                    }
                });
    }
}
