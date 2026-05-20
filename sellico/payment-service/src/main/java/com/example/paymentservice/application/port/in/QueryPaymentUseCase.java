package com.example.paymentservice.application.port.in;

import com.example.paymentservice.domain.model.Payment;

import java.util.UUID;

public interface QueryPaymentUseCase {
    Payment getById(UUID paymentId);
    Payment getByOrderId(UUID orderId);
}

