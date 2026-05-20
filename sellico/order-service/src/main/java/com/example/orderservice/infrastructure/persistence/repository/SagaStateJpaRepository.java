package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.infrastructure.persistence.entity.SagaStateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateJpaRepository extends JpaRepository<SagaStateJpaEntity, UUID> {

    Optional<SagaStateJpaEntity> findByOrderId(UUID orderId);
}

