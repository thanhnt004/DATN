package com.example.paymentservice.adapter.out.persistence.repository;

import com.example.paymentservice.adapter.out.persistence.entity.PaymentRefundJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRefundJpaRepository extends JpaRepository<PaymentRefundJpaEntity, UUID> {
    List<PaymentRefundJpaEntity> findByPaymentId(UUID paymentId);
}

