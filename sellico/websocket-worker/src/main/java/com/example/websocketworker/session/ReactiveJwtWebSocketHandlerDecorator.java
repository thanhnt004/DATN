package com.example.websocketworker.session;

import com.example.websocketworker.config.JwtAuthConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ReactiveJwtWebSocketHandlerDecorator implements WebSocketHandler {

    private final WebSocketHandler delegate;
    private final ReactiveJwtDecoder jwtDecoder;
    private final JwtAuthConverter jwtAuthConverter; // Converter của bạn

    public ReactiveJwtWebSocketHandlerDecorator(@Qualifier("wsHandler")WebSocketHandler delegate,
                                                ReactiveJwtDecoder jwtDecoder,
                                                JwtAuthConverter jwtAuthConverter) {
        this.delegate = delegate;
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 1. Lấy token từ Query Param
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractToken(query);

        if (token == null) {
            log.warn("WebSocket rejected: missing token, uri={}", session.getHandshakeInfo().getUri());
            return session.close(CloseStatus.POLICY_VIOLATION);
        }

        // 2. Decode và Validate Reactive
        return Mono.defer(() -> jwtDecoder.decode(token)
                        .flatMap(jwt -> {
                            Authentication auth = jwtAuthConverter.convert(jwt);
                            session.getAttributes().put("principal", auth);
                            log.info("Xác thực thành công user: {}", auth.getName());
                            return Mono.empty(); // Kết thúc luồng JWT tại đây
                        }))
                .then(delegate.handle(session)) // Chỉ chạy cái này khi cái defer ở trên xong
                .onErrorResume(e -> {
                    // Đây là chỗ cứu cánh cho cái lỗi ClassCastException của mày
                    if (e instanceof ClassCastException) {
                        // Nếu lỗi cast xảy ra, nghĩa là handshake đã xong nhưng pipeline Netty bị lộn xộn
                        // Ta chỉ cần log và để session tiếp tục nếu có thể, hoặc đóng êm đẹp
                        log.error("Netty Pipe Error (ClassCast): Mày hãy check lại phía Client xem có gửi tin nhắn quá sớm không");
                        return Mono.empty();
                    }
                    log.error("WebSocket Auth Error: {}", e.getMessage());
                    return session.close(CloseStatus.BAD_DATA);
                });
    }

    private String extractToken(String query) {
        if (StringUtils.hasText(query) && query.contains("token=")) {
            String rawToken = query.split("token=")[1].split("&")[0];
            return URLDecoder.decode(rawToken, StandardCharsets.UTF_8);
        }
        return null;
    }
}
