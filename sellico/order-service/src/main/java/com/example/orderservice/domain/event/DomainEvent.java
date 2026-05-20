package com.example.orderservice.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base Domain Event
 */
public abstract class DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.eventType = this.getClass().getSimpleName();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public String getEventType() {
        return eventType;
    }

    public abstract UUID getAggregateId();

    public String getAggregateType() {
        return "Order";
    }
}

