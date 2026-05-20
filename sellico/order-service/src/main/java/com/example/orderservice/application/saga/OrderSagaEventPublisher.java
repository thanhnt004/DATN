package com.example.orderservice.application.saga;

import com.example.orderservice.application.dto.response.OrderSagaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage SSE connections and publish saga events to clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaEventPublisher {

    private final ObjectMapper objectMapper;
    
    // Map of orderId -> SseEmitter
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    /**
     * Register a new SSE emitter for an order
     */
    public SseEmitter createEmitter(UUID orderId) {
        // 30 second timeout
        SseEmitter emitter = new SseEmitter(30_000L);
        
        emitter.onCompletion(() -> {
            log.info("[SSE] Emitter completed for order: {}", orderId);
            emitters.remove(orderId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("[SSE] Emitter timeout for order: {}", orderId);
            emitters.remove(orderId);
            emitter.complete();
        });
        
        emitter.onError((ex) -> {
            log.error("[SSE] Emitter error for order: {}", orderId, ex);
            emitters.remove(orderId);
        });
        
        emitters.put(orderId, emitter);
        log.info("[SSE] Emitter created for order: {}", orderId);
        
        return emitter;
    }
    
    /**
     * Publish a saga event to the client
     */
    public void publishEvent(OrderSagaEvent event) {
        UUID orderId = event.getOrderId();
        SseEmitter emitter = emitters.get(orderId);
        
        if (emitter == null) {
            log.debug("[SSE] No emitter found for order: {}", orderId);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .name("saga-event")
                    .data(json));
            
            log.info("[SSE] Event sent for order {}: status={}, step={}", 
                    orderId, event.getStatus(), event.getCurrentStep());
            
            // Complete emitter if saga is finished (completed or failed)
            if (event.getStatus() == OrderSagaEvent.SagaStatus.COMPLETED ||
                event.getStatus() == OrderSagaEvent.SagaStatus.FAILED ||
                event.getStatus() == OrderSagaEvent.SagaStatus.COMPENSATED) {
                
                emitter.complete();
                emitters.remove(orderId);
                log.info("[SSE] Emitter completed and removed for order: {}", orderId);
            }
            
        } catch (IOException e) {
            log.error("[SSE] Failed to send event for order: {}", orderId, e);
            emitters.remove(orderId);
            emitter.completeWithError(e);
        }
    }
    
    /**
     * Check if there's an active emitter for an order
     */
    public boolean hasActiveEmitter(UUID orderId) {
        return emitters.containsKey(orderId);
    }
    
    /**
     * Remove emitter for an order
     */
    public void removeEmitter(UUID orderId) {
        SseEmitter emitter = emitters.remove(orderId);
        if (emitter != null) {
            emitter.complete();
            log.info("[SSE] Emitter manually removed for order: {}", orderId);
        }
    }
}
