package com.example.websocketworker.routing;

import com.example.websocketworker.config.WorkerInfo;
import com.example.websocketworker.dto.WsMessage;
import com.example.websocketworker.kafka.KafkaProducer;
import com.example.websocketworker.redis.RedisService;
import com.example.websocketworker.session.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouterService {
    private final RedisService redisService;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final WorkerInfo workerInfo;
    private final KafkaProducer  kafkaProducer;
    public Mono<Void> pushToLocal(WsMessage<JsonNode> msg)
    {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                .flatMap(jsonPayload -> sessionRegistry.pushToUserLocal(msg.getTo(), jsonPayload));
    }
    public Mono<Void> routeFull(List<String> sessionIds, WsMessage<JsonNode> msg) {
        return  redisService.getWorkersOfSessions(sessionIds)
                .flatMap(workerIds  ->{
                    List<Mono<Void>> tasks = new ArrayList<>();
                    for (String workerId : workerIds) {
                        if (workerId.equals(workerInfo.getWorkerId())) {
                            tasks.add(
                                    Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                                            .flatMap(jsonPayload -> sessionRegistry.pushToUserLocal(msg.getTo(), jsonPayload))
                            );
                        } else {
                            tasks.add(forward(workerId, msg));
                        }
                    }
                    return Mono.when(tasks);
                })
                .then();
    }
    Mono<Void> forward(String workerId, WsMessage<JsonNode> msg) {
        return kafkaProducer.forwardToWorker( msg,workerId);
    }

}
