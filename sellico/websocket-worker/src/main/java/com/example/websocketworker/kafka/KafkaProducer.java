package com.example.websocketworker.kafka;

import com.example.websocketworker.dto.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {
    private final KafkaSender<String, String> sender;
    private final ObjectMapper objectMapper;
    private static final Map<String, String> TOPIC_ROUTES = Map.of(
            "CHAT", "ws.chat.inbound",
            "NOTIFICATION", "ws.notify.inbound",
            "LOCATION", "ws.tracking.inbound"
    );
    private static final String WORKER_TOPIC_ROUTES = "ws.delivery";
    public Mono<Void> forwardToKafka(String userId, WsMessage<JsonNode> msg) {
        return Mono.fromCallable(() -> {
                    msg.setFrom(userId);
                    return objectMapper.writeValueAsString(msg);
                })
                .flatMap(jsonPayload -> {
                    // 1. Lấy Feature để quyết định Topic
                    String feature = msg.getFeature().toString();

                    // 2. Lấy tên Topic, nếu Feature lạ thì cho vào Topic rác hoặc Topic mặc định
                    String targetTopic = TOPIC_ROUTES.getOrDefault(feature, "ws.unmapped.events");

                    // 3. Bắn đúng vào Topic đó
                    return sender.send(Mono.just(SenderRecord.create(new ProducerRecord<>(targetTopic, userId, jsonPayload), null)))
                            .then();
                });
    }
    public Mono<Void> forwardToWorker( WsMessage<JsonNode> msg,String workerId) {
        return Mono.fromCallable(() -> {
                    return objectMapper.writeValueAsString(msg);
                })
                .flatMap(jsonPayload -> {
                   //gui sang worker giu connect
                    return sender.send(Mono.just(SenderRecord.create(new ProducerRecord<>(WORKER_TOPIC_ROUTES, workerId, jsonPayload), null)))
                            .then();
                });
    }
}
