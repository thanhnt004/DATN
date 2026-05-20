package com.example.bannerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import configuration.JwtAuthConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Internal endpoints
                        .requestMatchers("/internal/**").permitAll()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Public: get active banners by position, click tracking
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/positions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/banners/active/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/banners/*/click").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/banners/*/view").permitAll()
                        // All admin endpoints require authentication
                        .anyRequest().authenticated()
                );

        // Only enable OAuth2 for authenticated endpoints
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }
}

