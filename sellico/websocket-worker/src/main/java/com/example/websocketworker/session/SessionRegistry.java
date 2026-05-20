package com.example.websocketworker.session;

import com.example.websocketworker.config.WorkerInfo;
import com.example.websocketworker.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionRegistry {

    private final ReactiveStringRedisTemplate redis;
    private final RedisService redisService;
    private final Map<String, UserChannel> channels = new ConcurrentHashMap<>();
    @Autowired
    private WorkerInfo workerInfo;

    // 2. Khi User kết nối: Tạo phễu cục bộ VÀ lưu vào Redis
    public Flux<String> registerAndGetStream(String userId, WebSocketSession session) {
        UserChannel channel = getOrCreate(userId);
        return redisService.addSessionInfo(session.getId(), workerInfo.getWorkerId(), userId)
                .doOnSuccess(v -> {
                    channel.addSession(session);
                    log.info("User {} connected to {}", userId, workerInfo.getWorkerId());
                })
                .thenMany(channel.getStream());
    }

    private UserChannel getOrCreate(String userId) {
        return channels.computeIfAbsent(userId, id -> new UserChannel());
    }
    public long getTotalSessions(String userId) {
        return channels.get(userId).getTotalSessions();
    }
    // 3. Khi User ngắt kết nối: Xóa phễu VÀ xóa khỏi Redis
    public Mono<Void> unregister(String userId, WebSocketSession session) {
        UserChannel channel = channels.get(userId);
        if (channel == null) return Mono.empty();

        channel.removeSession(session);

        Mono<Void> redisCleanup = redisService.removeSessionInfo(session.getId());

        Mono<Void> localCleanup = Mono.fromRunnable(() -> {
            if (channel.isEmpty()) {
                channel.close();
                channels.remove(userId);
            }
        });

        return redisCleanup
                .then(localCleanup)
                .doOnSuccess(v ->
                        log.info("User {} đã ngắt kết nối khỏi {}", userId, workerInfo.getWorkerId())
                );
    }

    // 4. Bơm data vào phễu để gửi xuống Web
    public Mono<Void> pushToUserLocal(String userId, String msg) {
        UserChannel channel = channels.get(userId);
        if (channel != null) {
            channel.emit(msg);
        }
        return Mono.empty();
    }
}