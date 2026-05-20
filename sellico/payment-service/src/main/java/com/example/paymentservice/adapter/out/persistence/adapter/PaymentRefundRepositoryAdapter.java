package com.example.paymentservice.adapter.out.persistence.adapter;

import com.example.paymentservice.adapter.out.persistence.mapper.PaymentPersistenceMapper;
import com.example.paymentservice.adapter.out.persistence.repository.PaymentRefundJpaRepository;
import com.example.paymentservice.application.port.out.PaymentRefundRepositoryPort;
import com.example.paymentservice.domain.model.PaymentRefund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRefundRepositoryAdapter implements PaymentRefundRepositoryPort {

    private final PaymentRefundJpaRepository repo;
    private final PaymentPersistenceMapper mapper;

    @Override
    public PaymentRefund save(PaymentRefund refund) {
        return mapper.toDomain(repo.save(mapper.toEntity(refund)));
    }

    @Override
    public List<PaymentRefund> findByPaymentId(UUID paymentId) {
        return repo.findByPaymentId(paymentId).stream().map(mapper::toDomain).toList();
    }
}

