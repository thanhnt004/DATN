package com.example.sellerservice.event;

import com.example.sellerservice.service.SellerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka consumer that listens to order events and updates seller statistics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final SellerService sellerService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.completed", groupId = "seller-service")
    public void handleOrderCompleted(String payload) {
        log.info("Received order.completed event in seller-service");

        try {
            JsonNode node = objectMapper.readTree(payload);

            JsonNode sellerIdNode = node.get("sellerId");
            if (sellerIdNode == null || sellerIdNode.isNull()) {
                log.warn("order.completed event has no sellerId, skipping");
                return;
            }

            UUID sellerId = UUID.fromString(sellerIdNode.asText());
            
            // Try to get totalAmount, if not present we might need to calculate it or just increment order count
            BigDecimal totalAmount = BigDecimal.ZERO;
            JsonNode totalAmountNode = node.get("totalAmount");
            if (totalAmountNode != null && !totalAmountNode.isNull()) {
                totalAmount = new BigDecimal(totalAmountNode.asText());
            } else {
                log.warn("order.completed event has no totalAmount for seller {}", sellerId);
            }

            sellerService.processOrderCompleted(sellerId, totalAmount);

        } catch (Exception e) {
            log.error("Failed to process order.completed event: {}", e.getMessage(), e);
        }
    }
}
