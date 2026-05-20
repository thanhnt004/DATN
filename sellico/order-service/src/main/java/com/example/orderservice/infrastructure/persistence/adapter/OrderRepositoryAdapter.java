package com.example.orderservice.infrastructure.persistence.adapter;

import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.port.output.OrderRepository;
import com.example.orderservice.infrastructure.persistence.entity.OrderJpaEntity;
import com.example.orderservice.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.example.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter that implements OrderRepository port
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findByIdWithItems(orderId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber)
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllByUserId(userId, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public List<Order> findByOrderIdIn(List<UUID> orderIds) {
        return jpaRepository.findAllByIdIn(orderIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllByUserIdAndStatus(userId, status, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public List<Order> findBySellerId(UUID sellerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllBySellerId(sellerId, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public List<Order> findBySellerIdAndStatus(UUID sellerId, OrderStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllBySellerIdAndStatus(sellerId, status, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public List<Order> findBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public List<Order> findBySellerIdAndStatusAndCreatedAtBetween(UUID sellerId, OrderStatus status, Instant startDate, Instant endDate, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jpaRepository.findAllBySellerIdAndStatusAndCreatedAtBetween(sellerId, status, startDate, endDate, pageRequest)
                .map(mapper::toDomain)
                .getContent();
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, OrderStatus status) {
        return jpaRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    public long countBySellerId(UUID sellerId) {
        return jpaRepository.countBySellerId(sellerId);
    }

    @Override
    public long countBySellerIdAndStatus(UUID sellerId, OrderStatus status) {
        return jpaRepository.countBySellerIdAndStatus(sellerId, status);
    }

    @Override
    public long countBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate) {
        return jpaRepository.countBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate);
    }

    @Override
    public long countBySellerIdAndStatusAndCreatedAtBetween(UUID sellerId, OrderStatus status, Instant startDate, Instant endDate) {
        return jpaRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, status, startDate, endDate);
    }

    @Override
    public void delete(Order order) {
        jpaRepository.deleteById(order.getId().value());
    }

    @Override
    public Map<OrderStatus, Long> countOrdersByStatus() {
        return jpaRepository.countOrdersByStatus().stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Map<OrderStatus, BigDecimal> sumRevenueByStatus() {
        return jpaRepository.sumRevenueByStatus().stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));
    }

    @Override
    public BigDecimal sumTotalCompletedRevenue() {
        BigDecimal result = jpaRepository.sumTotalCompletedRevenue();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public List<Object[]> getDailyRevenueReport(Instant startDate, Instant endDate) {
        return jpaRepository.getDailyRevenueReport(startDate, endDate);
    }

    @Override
    public Map<OrderStatus, Long> countSellerOrdersByStatus(UUID sellerId) {
        return jpaRepository.countSellerOrdersByStatus(sellerId).stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Map<OrderStatus, BigDecimal> sumSellerRevenueByStatus(UUID sellerId) {
        return jpaRepository.sumSellerRevenueByStatus(sellerId).stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO
                ));
    }

    @Override
    public BigDecimal sumSellerCompletedRevenue(UUID sellerId) {
        BigDecimal result = jpaRepository.sumSellerCompletedRevenue(sellerId);
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public List<Object[]> getSellerDailyRevenueReport(UUID sellerId, Instant startDate, Instant endDate) {
        return jpaRepository.getSellerDailyRevenueReport(sellerId, startDate, endDate);
    }

    @Override
    public List<Order> findBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate) {
        return jpaRepository.findAllBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

