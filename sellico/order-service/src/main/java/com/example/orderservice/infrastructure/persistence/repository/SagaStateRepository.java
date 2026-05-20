package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.domain.model.OrderSagaState;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.infrastructure.persistence.entity.SagaStateJpaEntity;
import com.example.orderservice.infrastructure.persistence.mapper.SagaStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SagaStateRepository {

    private final SagaStateJpaRepository jpaRepository;
    private final SagaStateMapper mapper;

    public OrderSagaState save(OrderSagaState sagaState) {
        SagaStateJpaEntity entity = mapper.toEntity(sagaState);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    public Optional<OrderSagaState> findByOrderId(OrderId orderId) {
        return jpaRepository.findByOrderId(orderId.value())
                .map(mapper::toDomain);
    }
}

