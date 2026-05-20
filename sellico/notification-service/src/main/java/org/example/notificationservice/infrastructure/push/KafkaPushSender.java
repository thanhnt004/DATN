package org.example.notificationservice.infrastructure.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.domain.port.out.PushSenderPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaPushSender implements PushSenderPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String WS_OUTBOUND_TOPIC = "ws.outbound";

    @Override
    public SendPushResult sendPush(SendPushRequest request) {
        log.info("Sending push notification to user: {} via Kafka", request.getUserId());

        try {
            WsMessage<Object> wsMessage = new WsMessage<>();
            wsMessage.setTraceId(UUID.randomUUID().toString());
            wsMessage.setFeature("NOTIFICATION");
            wsMessage.setAction("SEND");
            wsMessage.setFrom("notification-service");
            wsMessage.setTo(request.getUserId());
            wsMessage.setTimestamp(System.currentTimeMillis());
            wsMessage.setPayload(request.getPayload() != null ? request.getPayload() : request.getBody());

            kafkaTemplate.send(WS_OUTBOUND_TOPIC, request.getUserId(), wsMessage);
            
            return SendPushResult.success(wsMessage.getTraceId());
        } catch (Exception e) {
            log.error("Failed to send push notification via Kafka: {}", e.getMessage(), e);
            return SendPushResult.failure("KAFKA_ERROR", e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "WEBSOCKET";
    }

    /**
     * DTO matching websocket-worker's WsMessage
     */
    @lombok.Data
    private static class WsMessage<T> {
        private String traceId;
        private String feature; // Using String to avoid direct enum dependency or for flexibility
        private String action;
        private String from;
        private String to;
        private long timestamp;
        private T payload;
    }
}
