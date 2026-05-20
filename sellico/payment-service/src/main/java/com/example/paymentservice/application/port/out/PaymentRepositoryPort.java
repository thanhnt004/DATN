package com.example.paymentservice.application.port.out;

import com.example.paymentservice.domain.model.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);
    List<Payment> findPendingPaymentsCreatedBefore(Instant cutoff);
    boolean existsPendingOrCompletedByOrderId(UUID orderId);
}

