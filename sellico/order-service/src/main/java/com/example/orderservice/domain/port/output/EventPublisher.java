package com.example.orderservice.domain.port.output;

import com.example.orderservice.domain.event.DomainEvent;

/**
 * Output Port - Event Publisher Interface
 * Implemented by Infrastructure layer (Outbox Pattern)
 */
public interface EventPublisher {

    /**
     * Save event to outbox table (within same transaction)
     */
    void publish(DomainEvent event);
}

