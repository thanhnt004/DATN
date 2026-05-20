package com.example.sellerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import configuration.JwtAuthConverter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Internal service-to-service endpoints — no auth
                        .requestMatchers("/internal/**").permitAll()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Public: list sellers, search, top-rated, get by ID/slug
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/top-rated").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/slug/{slug}").permitAll()
                        // Authenticated endpoints that might match {id} pattern — must be listed BEFORE {id}
                        .requestMatchers("/api/v1/sellers/register/**").authenticated()
                        .requestMatchers("/api/v1/sellers/following").authenticated()
                        // Public: get seller by ID
                        .requestMatchers(HttpMethod.GET, "/api/v1/sellers/{id}").permitAll()
                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }
}

