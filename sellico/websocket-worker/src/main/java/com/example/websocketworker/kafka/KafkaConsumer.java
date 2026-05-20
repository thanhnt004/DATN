package com.example.websocketworker.kafka;

import com.example.websocketworker.dto.WsMessage;
import com.example.websocketworker.routing.RouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final ReactiveStringRedisTemplate redis;
    private final RouterService routerService;
    private final ObjectMapper objectMapper;
    private final ReceiverOptions<String, byte[]> kafkaReceiverOptions;

    @EventListener(ApplicationStartedEvent.class)
    public void onMessage() {
        ReceiverOptions<String, byte[]> options = kafkaReceiverOptions
                .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "ws-outbound-group")
                .subscription(List.of("ws.outbound"));
        KafkaReceiver.create(options)
                .receive()
                .concatMap(record ->
                        handleOutBoundMessage(record)
                                .doOnSuccess(v -> record.receiverOffset().acknowledge())
                                .onErrorResume(e -> {
                                    log.error("Kafka process error", e);
                                    record.receiverOffset().acknowledge();
                                    return Mono.empty();
                                })
                )
                .subscribe();
    }
    private Mono<Void> handleOutBoundMessage(ReceiverRecord<String, byte[]> record) {
        return Mono.fromCallable(() ->
                        objectMapper.readValue(record.value(),
                                new TypeReference<WsMessage<JsonNode>>() {})
                ).subscribeOn(Schedulers.boundedElastic())
                .flatMap(msg -> {
                    String userId = msg.getTo();
                    return redis.opsForSet()
                                .members("ws:user:" + userId)
                                .collectList()
                                .flatMap(sessions -> {
                                    log.info("Routing outbound ws message traceId={} kafkaKey={} toUser={} sessions={}",
                                            msg.getTraceId(), record.key(), userId, sessions.size());
                                    return routerService.routeFull(sessions, msg);
                                });
                })
                .onErrorResume(e -> {
                    log.error("Deserialize failed", e);
                    return Mono.empty();
                });

    }
}
