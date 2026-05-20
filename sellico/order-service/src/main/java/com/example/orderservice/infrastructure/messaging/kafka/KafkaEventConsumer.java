package com.example.orderservice.infrastructure.messaging.kafka;

import com.example.orderservice.application.dto.command.ProcessPaymentCommand;
import com.example.orderservice.application.port.input.PaymentUseCase;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kafka Event Consumer - listens to events from other services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final PaymentUseCase paymentUseCase;
    private final ObjectMapper objectMapper;

    /**
     * Listen to payment completed events from payment-service
     */
    @KafkaListener(topics = "payment.completed", groupId = "order-service")
    public void handlePaymentCompleted(String payload) {
        log.info("Received payment.completed event: {}", payload);

        try {
            JsonNode node = objectMapper.readTree(payload);

            ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                    .orderId(UUID.fromString(node.get("orderId").asText()))
                    .paymentMethod(PaymentMethod.valueOf(node.get("paymentMethod").asText()))
                    .amount(new BigDecimal(node.get("amount").asText()))
                    .transactionId(node.get("transactionId").asText())
                    .paymentGateway(node.has("paymentGateway") ? node.get("paymentGateway").asText() : null)
                    .build();

            paymentUseCase.processPayment(command);
            log.info("Payment processed for order: {}", command.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process payment.completed event", e);
            // Could send to DLQ here
        }
    }

    /**
     * Listen to inventory reserved events from inventory-service
     */
    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    public void handleInventoryReserved(String payload) {
        log.info("Received inventory.reserved event: {}", payload);
        // Handle inventory reservation confirmation
    }

    /**
     * Listen to inventory reservation failed events
     */
    @KafkaListener(topics = "inventory.reservation.failed", groupId = "order-service")
    public void handleInventoryReservationFailed(String payload) {
        log.warn("Received inventory.reservation.failed event: {}", payload);
        // Trigger saga compensation
    }
}

