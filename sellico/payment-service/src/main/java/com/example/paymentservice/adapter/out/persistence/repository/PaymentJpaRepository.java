package com.example.paymentservice.adapter.out.persistence.repository;

import com.example.paymentservice.adapter.out.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByOrderId(UUID orderId);

    Optional<PaymentJpaEntity> findByVnpayTxnRef(String vnpayTxnRef);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaymentJpaEntity p " +
           "WHERE p.orderId = :orderId AND p.status IN ('PENDING', 'COMPLETED', 'COD_PENDING', 'COD_CONFIRMED')")
    boolean existsPendingOrCompletedByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.status = 'PENDING' AND p.createdAt < :cutoff")
    List<PaymentJpaEntity> findPendingCreatedBefore(@Param("cutoff") Instant cutoff);
}

