package com.example.websocketworker.kafka;

import com.example.websocketworker.config.WorkerInfo;
import com.example.websocketworker.dto.WsMessage;
import com.example.websocketworker.routing.RouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryConsumer {

    private final ReceiverOptions<String, byte[]> kafkaReceiverOptions;
    private final ObjectMapper objectMapper;
    private final RouterService routerService;
    private final WorkerInfo workerInfo;
    // inject từ config / env
    @EventListener(ApplicationStartedEvent.class)
    public void start() {
        String workerId = workerInfo.getWorkerId();
        ReceiverOptions<String, byte[]> options = kafkaReceiverOptions
                .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, "ws-delivery-" + workerId)
                .subscription(List.of("ws.delivery"));

        KafkaReceiver.create(options)
                .receive()
                .concatMap(this::handleDeliveredMessage) // giữ order theo worker
                .subscribe(
                        null,
                        e -> log.error("Delivery consumer crashed", e)
                );
    }
    private Mono<Void> handleDeliveredMessage(ReceiverRecord<String, byte[]> record) {
        String workerId = workerInfo.getWorkerId();
        if (!workerId.equals(record.key())) {
            return Mono.empty();
        }
        return Mono.fromCallable(() ->
                        objectMapper.readValue(record.value(),
                                new TypeReference<WsMessage<JsonNode>>() {})
                ).subscribeOn(Schedulers.boundedElastic())
                .flatMap(routerService::pushToLocal)
                .then(Mono.fromRunnable(record.receiverOffset()::acknowledge))
                .onErrorResume(e -> {
                    log.error("Deserialize failed", e);
                    return sendToDlq(record);
                }).then();
    }

    private Mono<Void> sendToDlq(ReceiverRecord<String, byte[]> record) {
        // TODO
        return Mono.empty();
    }
}