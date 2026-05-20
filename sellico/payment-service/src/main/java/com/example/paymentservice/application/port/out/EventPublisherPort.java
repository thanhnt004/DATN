package com.example.paymentservice.application.port.out;

public interface EventPublisherPort {
    void publishPaymentCompleted(String aggregateId, String payload);
}

