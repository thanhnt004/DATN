package com.example.websocketworker.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final ReactiveStringRedisTemplate redis;
    public Mono<Void> addSessionInfo(String sessionId,String workerId,String userId)
    {
        return redis.opsForHash().putAll(
                "ws:session:" + sessionId,
                Map.of(
                        "userId", userId,
                        "workerId", workerId
                )
        ).then(
                redis.opsForSet().add("ws:user:" + userId, sessionId)
        ).then();
    }
    public Mono<Void> removeSessionInfo(String sessionId)
    {
        String key = "ws:session:" + sessionId;
        return redis.opsForHash().get(key, "userId")
                .cast(String.class)
                .flatMap(userId ->
                        redis.opsForSet()
                                .remove("ws:user:" + userId, sessionId)
                                .then()
                )
                .then(redis.delete(key))
                .then();
    }

    public Mono<Set<String>> getWorkersOfSessions(List<String> sessionIds) {

        return Flux.fromIterable(sessionIds)
                .flatMap(id ->
                        redis.opsForHash()
                                .get("ws:session:" + id, "workerId")
                )
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }
}
