package org.example.notificationservice.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import event.EventMetadata;
import event.EventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.domain.exception.NotificationException;
import org.example.notificationservice.domain.model.Priority;
import org.example.notificationservice.domain.port.in.SendNotificationUseCase;
import org.example.notificationservice.domain.port.in.command.SendEmailCommand;
import org.example.notificationservice.domain.port.in.command.SendPushCommand;
import org.example.notificationservice.infrastructure.kafka.event.EventTypes;
import org.example.notificationservice.infrastructure.kafka.event.OrderEvents.*;
import org.example.notificationservice.infrastructure.kafka.event.UserEvents.*;
import org.example.notificationservice.infrastructure.kafka.event.SellerEvents.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ObjectMapper kafkaObjectMapper;

    @KafkaListener(topics = {"${kafka.topics.order-events:order-events}", "order.confirmed", "order.shipped", "order.delivered", "order.cancelled"}, groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void handleOrderEvent(String rawEvent) {
        EventWrapper<Map<String, Object>> event = null;
        String eventType = null;
        String eventId = null;

        try {
            try {
                event = parseEventWrapper(rawEvent);
                eventType = event.metadata().eventType();
                eventId = event.metadata().eventId();
            } catch (IllegalArgumentException wrapperEx) {
                Map<String, Object> rawMap = parseOrderEventPayload(rawEvent);
                eventType = rawMap.get("eventType") == null ? null : rawMap.get("eventType").toString();
                eventId = rawMap.get("eventId") == null ? null : rawMap.get("eventId").toString();

                Map<String, Object> payload = new HashMap<>(rawMap);
                payload.remove("eventType");
                payload.remove("eventId");
                payload.remove("occurredOn");
                payload.remove("aggregateId");
                payload.remove("aggregateType");

                if (payload.containsKey("payload")) {
                    Object nestedPayload = payload.get("payload");
                    if (nestedPayload instanceof Map) {
                        payload = new HashMap<>((Map<String, Object>) nestedPayload);
                    }
                }

                event = new EventWrapper<>(new EventMetadata(eventId, eventType, null, null), payload);
            }

            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("Order event payload missing eventType");
            }

            log.info("Received order event: type={}, eventId={}", eventType, eventId);

            switch (eventType) {
                case EventTypes.ORDER_CREATED -> handleOrderCreated(event);
                case EventTypes.ORDER_CONFIRMED -> handleOrderConfirmed(event);
                case EventTypes.ORDER_SHIPPED -> handleOrderShipped(event);
                case EventTypes.ORDER_DELIVERED -> handleOrderDelivered(event);
                case EventTypes.ORDER_CANCELLED -> handleOrderCancelled(event);
                default -> log.warn("Unknown order event type: {}", eventType);
            }
        } catch (IllegalArgumentException | NotificationException e) {
            log.error("Skipping invalid order event type={} eventId={} payload={} error={}",
                    event == null ? "unknown" : event.metadata().eventType(),
                    eventId,
                    event == null ? rawEvent : event.payload(),
                    e.getMessage(), e);
            // Skip retry for malformed order payloads or known notification configuration issues
        } catch (Exception e) {
            log.error("Failed to process order event type={} eventId={} payload={} error={}",
                    event == null ? "unknown" : event.metadata().eventType(),
                    eventId,
                    event == null ? rawEvent : event.payload(),
                    e.getMessage(), e);
            throw e; // Re-throw to allow Kafka error handler retry
        }
    }

    @KafkaListener(topics = "${kafka.topics.user-events:user-events}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void handleUserEvent(String rawEvent) {
        EventWrapper<Map<String, Object>> event = parseEventWrapper(rawEvent);
        log.info("Received user event: type={}, eventId={}",
                event.metadata().eventType(), event.metadata().eventId());

        try {
            String eventType = event.metadata().eventType();

            switch (eventType) {
                case EventTypes.USER_REGISTERED -> handleUserRegistered(event);
                case EventTypes.PASSWORD_RESET_REQUESTED -> handlePasswordResetRequested(event);
                default -> log.warn("Unknown user event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ==================== Order Event Handlers ====================

    private void handleOrderCreated(EventWrapper<?> event) {
        OrderCreatedPayload payload = convertPayload(event.payload(), OrderCreatedPayload.class);
        validateOrderPayload(payload);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("ORDER_CREATED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.HIGH)
                .build());

        sendNotificationUseCase.sendPush(SendPushCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .notificationType("ORDER_CREATED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.HIGH)
                .build());
    }

    private void handleOrderConfirmed(EventWrapper<?> event) {
        OrderConfirmedPayload payload = convertPayload(event.payload(), OrderConfirmedPayload.class);
        validateOrderPayload(payload);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("ORDER_CONFIRMED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());

        sendNotificationUseCase.sendPush(SendPushCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .notificationType("ORDER_CONFIRMED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());
    }

    private void handleOrderShipped(EventWrapper<?> event) {
        OrderShippedPayload payload = convertPayload(event.payload(), OrderShippedPayload.class);
        validateOrderPayload(payload);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("ORDER_SHIPPED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());

        sendNotificationUseCase.sendPush(SendPushCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .notificationType("ORDER_SHIPPED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());
    }

    private void handleOrderDelivered(EventWrapper<?> event) {
        OrderDeliveredPayload payload = convertPayload(event.payload(), OrderDeliveredPayload.class);
        validateOrderPayload(payload);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("ORDER_DELIVERED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());

        sendNotificationUseCase.sendPush(SendPushCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .notificationType("ORDER_DELIVERED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.NORMAL)
                .build());
    }

    private void handleOrderCancelled(EventWrapper<?> event) {
        OrderCancelledPayload payload = convertPayload(event.payload(), OrderCancelledPayload.class);
        validateOrderPayload(payload);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("ORDER_CANCELLED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.HIGH)
                .build());

        sendNotificationUseCase.sendPush(SendPushCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .notificationType("ORDER_CANCELLED")
                .payload(toMap(payload))
                .referenceId(payload.getOrderId())
                .priority(Priority.HIGH)
                .build());
    }

    // ==================== User Event Handlers ====================

    private void handleUserRegistered(EventWrapper<?> event) {
        UserRegisteredPayload payload = convertPayload(event.payload(), UserRegisteredPayload.class);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("WELCOME")
                .payload(toMap(payload))
                .priority(Priority.NORMAL)
                .build());
    }

    private void handlePasswordResetRequested(EventWrapper<?> event) {
        PasswordResetRequestedPayload payload = convertPayload(event.payload(), PasswordResetRequestedPayload.class);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType("PASSWORD_RESET")
                .payload(toMap(payload))
                .priority(Priority.HIGH)
                .build());
    }

    // ==================== Helper Methods ====================

    private <T> T convertPayload(Object payload, Class<T> targetType) {
        return kafkaObjectMapper.convertValue(payload, targetType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object obj) {
        return kafkaObjectMapper.convertValue(obj, Map.class);
    }

    private EventWrapper<Map<String, Object>> parseEventWrapper(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            throw new IllegalArgumentException("Received empty Kafka event payload");
        }
        try {
            return kafkaObjectMapper.readValue(rawEvent, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Kafka event wrapper payload", e);
        }
    }

    private Map<String, Object> parseOrderEventPayload(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            throw new IllegalArgumentException("Received empty Kafka event payload");
        }
        try {
            return kafkaObjectMapper.readValue(rawEvent, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Kafka order event payload", e);
        }
    }

    private void validateOrderPayload(Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Order payload is missing");
        }

        Map<String, Object> payloadMap = kafkaObjectMapper.convertValue(payload, Map.class);
        String userId = payloadMap.get("userId") == null ? null : payloadMap.get("userId").toString();
        String email = payloadMap.get("email") == null ? null : payloadMap.get("email").toString();
        String orderId = payloadMap.get("orderId") == null ? null : payloadMap.get("orderId").toString();

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Order payload missing userId");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Order payload missing email");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order payload missing orderId");
        }
        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Order payload userId is not a valid UUID", ex);
        }
    }

    // ==================== Seller Event Handlers ====================

    @KafkaListener(topics = "${kafka.topics.seller-events:seller-events}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void handleSellerEvent(String rawEvent) {
        EventWrapper<Map<String, Object>> event = parseEventWrapper(rawEvent);
        log.info("Received seller event: type={}, eventId={}",
                event.metadata().eventType(), event.metadata().eventId());

        try {
            String eventType = event.metadata().eventType();

            switch (eventType) {
                case EventTypes.SELLER_APPROVED -> handleSellerStatusChanged(event, "SELLER_APPROVED");
                case EventTypes.SELLER_REJECTED -> handleSellerStatusChanged(event, "SELLER_REJECTED");
                case EventTypes.SELLER_SUSPENDED -> handleSellerStatusChanged(event, "SELLER_SUSPENDED");
                case EventTypes.SELLER_BANNED -> handleSellerStatusChanged(event, "SELLER_BANNED");
                case EventTypes.SELLER_REACTIVATED -> handleSellerStatusChanged(event, "SELLER_REACTIVATED");
                default -> log.warn("Unknown seller event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process seller event: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleSellerStatusChanged(EventWrapper<?> event, String notificationType) {
        SellerStatusChangedPayload payload = convertPayload(event.payload(), SellerStatusChangedPayload.class);

        sendNotificationUseCase.sendEmail(SendEmailCommand.builder()
                .userId(UUID.fromString(payload.getUserId()))
                .recipientEmail(payload.getEmail())
                .notificationType(notificationType)
                .payload(toMap(payload))
                .referenceId(payload.getSellerId())
                .priority(Priority.HIGH)
                .build());
    }
}

