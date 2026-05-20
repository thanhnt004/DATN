package com.example.websocketworker.session;

import com.example.websocketworker.config.JwtAuthConverter;
import com.example.websocketworker.config.WorkerInfo;
import com.example.websocketworker.dto.WsMessage;
import com.example.websocketworker.kafka.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.Authentication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
@RequiredArgsConstructor
public class WsHandler implements WebSocketHandler {
    private final KafkaProducer kafkaProducer;
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final ReactiveJwtDecoder jwtDecoder; // Inject thêm thằng này vào
    private final JwtAuthConverter jwtAuthConverter; // Và thằng này nữa
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractToken(query);

        if (token == null) {
            return session.close(CloseStatus.POLICY_VIOLATION);
        }
//        Mono<Void> inbound = session.receive()
//                .flatMap(msg ->
//                        parse(msg)
//                                .flatMap(parsed -> {
//                                    log.info("Inbound websocket message traceId={} feature={} action={} from={} to={}",
//                                            parsed.getTraceId(), parsed.getFeature(), parsed.getAction(), userId, parsed.getTo());
//                                    return kafkaProducer.forwardToKafka(userId, parsed);
//                                })
//                )
//                .onErrorContinue((e, obj) ->
//                        log.error("Parse error user {}", userId, e)
//                )
//                .then();
//
//        Mono<Void> outbound = session.send(
//                sessionRegistry.registerAndGetStream(userId,session)
//                        .map(session::textMessage) // Chuyển String thành WebSocketMessage
//        );
//
//        Mono<Void> lifecycle = Mono.when(outbound, inbound).then();
//
//        return lifecycle
//                .then(Mono.defer(() -> sessionRegistry.unregister(userId, session)));
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    Authentication auth = jwtAuthConverter.convert(jwt);
                    String userId = auth.getName();
                    log.info("User {} connected", userId);

                    // Logic xử lý Inbound/Outbound của mày đưa hết vào đây
                    Mono<Void> inbound = session.receive()
                            .flatMap(msg -> parse(msg)
                                    .flatMap(parsed -> kafkaProducer.forwardToKafka(userId, parsed)))
                            .onErrorContinue((e, obj) -> log.error("Parse error user {}", userId, e))
                            .then();

                    Mono<Void> outbound = session.send(
                            sessionRegistry.registerAndGetStream(userId, session)
                                    .map(session::textMessage)
                    );

                    return Mono.when(inbound, outbound)
                            .then(Mono.defer(() -> sessionRegistry.unregister(userId, session)));
                })
                .onErrorResume(e -> {
                    log.error("Auth/Session error: {}", e.getMessage());
                    return session.close(CloseStatus.BAD_DATA);
                });
    }
    private String extract(WebSocketSession session) {
        Authentication auth = (Authentication) session.getAttributes().get("principal");
        if (auth != null) {
            String currentUserId = auth.getName(); // getName() mặc định trả về 'sub' của JWT
            return currentUserId;
        }
        return null;
    }
    private Mono<WsMessage<JsonNode>> parse(WebSocketMessage msg) {
        return Mono.fromCallable(() ->
                objectMapper.readValue(
                        msg.getPayloadAsText(),
                        new TypeReference<WsMessage<JsonNode>>() {}
                )
        );
    }
    private String extractToken(String query) {
        if (StringUtils.hasText(query) && query.contains("token=")) {
            String rawToken = query.split("token=")[1].split("&")[0];
            return URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
        }
        return null;
    }
}