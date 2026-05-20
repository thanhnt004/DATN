package com.example.orderservice.infrastructure.client;

import com.example.orderservice.domain.port.output.CartPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cart Service Adapter - implements CartPort
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartServiceAdapter implements CartPort {

    private final CartFeignClient cartClient;

    @Override
    public void clearSelectedItems(UUID userId) {
        try {
            cartClient.clearSelectedItems(userId);
            log.info("Cleared selected items from cart for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to clear cart for user: {}", userId, e);
        }
    }
}

