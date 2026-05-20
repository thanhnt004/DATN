package com.example.paymentservice.adapter.out.persistence.adapter;

import com.example.paymentservice.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.example.paymentservice.adapter.out.persistence.repository.PaymentJpaRepository;
import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository repo;
    private final PaymentPersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        return mapper.toDomain(repo.save(mapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repo.findByOrderId(orderId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef) {
        return repo.findByVnpayTxnRef(vnpayTxnRef).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findPendingPaymentsCreatedBefore(Instant cutoff) {
        return repo.findPendingCreatedBefore(cutoff).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsPendingOrCompletedByOrderId(UUID orderId) {
        return repo.existsPendingOrCompletedByOrderId(orderId);
    }
}

