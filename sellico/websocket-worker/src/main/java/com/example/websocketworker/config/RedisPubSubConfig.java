package com.example.websocketworker.config;

import com.example.websocketworker.session.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RedisPubSubConfig {
    private final WorkerInfo workerInfo;
    @Bean
    public ReactiveRedisMessageListenerContainer redisMessageListenerContainer(
            ReactiveRedisConnectionFactory connectionFactory,
            SessionRegistry sessionRegistry,
            ObjectMapper objectMapper) {

        ReactiveRedisMessageListenerContainer container = new ReactiveRedisMessageListenerContainer(connectionFactory);

        // Kênh riêng của Node này (Ví dụ: channel:node-a1b2)
        String myNodeChannel = "channel:" + workerInfo.getWorkerId();

        container.receive(ChannelTopic.of(myNodeChannel))
                .map(message -> message.getMessage()) // Lấy chuỗi JSON
                .flatMap(jsonMessage -> {
                    try {
                        // Trích xuất receiverId từ JSON
                        JsonNode root = objectMapper.readTree(jsonMessage);
                        String receiverId = root.path("to").asText();

                        // Đẩy xuống client (chắc chắn client đang ở đây vì nó đã bị định tuyến vào kênh này)
                        return sessionRegistry.pushToUserLocal(receiverId, jsonMessage);

                    } catch (Exception e) {
                        log.error("Lỗi xử lý tin nhắn Redis PubSub", e);
                        return Mono.empty();
                    }
                }
                )
                .subscribe(); // Khởi động listener

        log.info("Node đã mở kênh nhận tin nhắn: {}", myNodeChannel);
        return container;
    }
}
