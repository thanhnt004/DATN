package com.example.productservice.adapter.in.messaging;

import com.example.productservice.application.command.UpdateSoldCountCommand;
import com.example.productservice.application.port.in.UpdateSoldCountUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer that listens to order events and updates product sold counts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final UpdateSoldCountUseCase updateSoldCountUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.completed", groupId = "product-service")
    public void handleOrderCompleted(String payload) {
        log.info("Received order.completed event");

        try {
            JsonNode node = objectMapper.readTree(payload);

            JsonNode items = node.get("items");
            if (items == null || !items.isArray() || items.isEmpty()) {
                log.warn("order.completed event has no items, skipping sold count update");
                return;
            }

            for (JsonNode item : items) {
                UUID productId = UUID.fromString(item.get("productId").asText());
                int quantity = item.get("quantity").asInt();

                UpdateSoldCountCommand command = UpdateSoldCountCommand.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build();

                updateSoldCountUseCase.updateSoldCount(command);
                log.info("Updated sold count: productId={}, quantity=+{}", productId, quantity);
            }

        } catch (Exception e) {
            log.error("Failed to process order.completed event: {}", e.getMessage(), e);
        }
    }
}
