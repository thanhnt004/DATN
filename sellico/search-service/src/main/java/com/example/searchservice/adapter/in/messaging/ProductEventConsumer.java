package com.example.searchservice.adapter.in.messaging;

import com.example.searchservice.application.port.in.IndexProductUseCase;
import com.example.searchservice.domain.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final IndexProductUseCase indexProductUseCase;

    @KafkaListener(
            topics = "${app.kafka.topic.product-events:product-events}",
            groupId = "${spring.kafka.consumer.group-id:search-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload ProductEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received product event from topic={}, partition={}, offset={}, type={}",
                topic, partition, offset, event.getEventType());
        try {
            indexProductUseCase.handleProductEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process product event: productId={}, error={}",
                    event.getProductId(), e.getMessage(), e);
            // Nack → retry theo cấu hình retry của Kafka
        }
    }
}
