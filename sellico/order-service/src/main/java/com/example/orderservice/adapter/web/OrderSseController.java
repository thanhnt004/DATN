package com.example.orderservice.adapter.web;

import com.example.orderservice.application.saga.OrderSagaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * SSE Controller for real-time order saga status updates
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderSseController {

    private final OrderSagaEventPublisher eventPublisher;

    /**
     * SSE endpoint for order saga status updates
     * Frontend subscribes to this endpoint after creating an order
     * 
     * GET /api/v1/orders/{orderId}/saga-status
     */
    @GetMapping(value = "/{orderId}/saga-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeSagaStatus(@PathVariable("orderId") UUID orderId) {
        log.info("[SSE-Controller] Client subscribing to saga status for order: {}", orderId);
        
        SseEmitter emitter = eventPublisher.createEmitter(orderId);
        
        return emitter;
    }
}
