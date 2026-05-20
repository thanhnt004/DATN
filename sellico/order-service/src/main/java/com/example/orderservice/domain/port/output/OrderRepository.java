package com.example.orderservice.domain.port.output;

import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.model.valueobject.OrderId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port - Order Repository Interface
 * Implemented by Infrastructure layer
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(UUID userId, int page, int size);
    List<Order> findByOrderIdIn(List<UUID> orderIds);
    List<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, int page, int size);

    List<Order> findBySellerId(UUID sellerId, int page, int size);

    List<Order> findBySellerIdAndStatus(UUID sellerId, OrderStatus status, int page, int size);

    List<Order> findBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate, int page, int size);

    List<Order> findBySellerIdAndStatusAndCreatedAtBetween(UUID sellerId, OrderStatus status, Instant startDate, Instant endDate, int page, int size);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, OrderStatus status);

    long countBySellerId(UUID sellerId);

    long countBySellerIdAndStatus(UUID sellerId, OrderStatus status);

    long countBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate);

    long countBySellerIdAndStatusAndCreatedAtBetween(UUID sellerId, OrderStatus status, Instant startDate, Instant endDate);

    void delete(Order order);

    Map<OrderStatus, Long> countOrdersByStatus();

    Map<OrderStatus, BigDecimal> sumRevenueByStatus();

    BigDecimal sumTotalCompletedRevenue();

    List<Object[]> getDailyRevenueReport(Instant startDate, Instant endDate);

    Map<OrderStatus, Long> countSellerOrdersByStatus(UUID sellerId);

    Map<OrderStatus, BigDecimal> sumSellerRevenueByStatus(UUID sellerId);

    BigDecimal sumSellerCompletedRevenue(UUID sellerId);

    List<Object[]> getSellerDailyRevenueReport(UUID sellerId, Instant startDate, Instant endDate);

    List<Order> findBySellerIdAndCreatedAtBetween(UUID sellerId, Instant startDate, Instant endDate);
}

