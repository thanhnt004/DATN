package com.example.websocketworker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class ReactiveSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // JWT validation for chat websocket is handled inside ReactiveJwtWebSocketHandlerDecorator.
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/ws/**", "/actuator/**").permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
    @Bean
    public WebSessionManager webSessionManager() {
        // Tắt quản lý session của web để tránh xung đột với WS session
        return exchange -> Mono.empty();
    }
}
