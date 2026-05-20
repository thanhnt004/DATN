package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.exception.OrderNotFoundException;
import com.example.orderservice.application.mapper.OrderApplicationMapper;
import com.example.orderservice.application.port.input.QueryOrderUseCase;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.enums.OrderStatus;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.port.output.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query Order Service - Implements QueryOrderUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueryOrderService implements QueryOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderApplicationMapper mapper;

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return mapper.toResponse(order);
    }

    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderNumber));
        return mapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByIds(List<UUID> orderIds) {
        return orderRepository.findByOrderIdIn(orderIds).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderPageResponse<OrderResponse> getUserOrders(UUID userId, OrderStatus status, int page, int size) {
        List<Order> orders;
        long totalElements;
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(userId, status, page, size);
            totalElements = orderRepository.countByUserIdAndStatus(userId, status);
        } else {
            orders = orderRepository.findByUserId(userId, page, size);
            totalElements = orderRepository.countByUserId(userId);
        }
        List<OrderResponse> content = orders.stream().map(mapper::toResponse).toList();
        return OrderPageResponse.of(content, page, size, totalElements);
    }

    @Override
    public OrderPageResponse<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, int page, int size) {
        return getSellerOrders(sellerId, status, null, null, null, page, size);
    }

    @Override
    public OrderPageResponse<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, String orderNumber, LocalDate startDate, LocalDate endDate, int page, int size) {
        if (orderNumber != null && !orderNumber.isBlank()) {
            try {
                OrderResponse order = getOrderByNumber(orderNumber.trim());
                if (!order.getSellerId().equals(sellerId)) {
                    return OrderPageResponse.of(List.of(), page, size, 0);
                }
                return OrderPageResponse.of(List.of(order), page, size, 1);
            } catch (RuntimeException ex) {
                return OrderPageResponse.of(List.of(), page, size, 0);
            }
        }

        boolean useDateRange = startDate != null || endDate != null;
        Instant startInstant = null;
        Instant endInstant = null;
        if (useDateRange) {
            LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            startInstant = start.atStartOfDay(ZoneId.systemDefault()).toInstant();
            endInstant = end.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        }

        List<Order> orders;
        long totalElements;
        if (useDateRange) {
            if (status != null) {
                orders = orderRepository.findBySellerIdAndStatusAndCreatedAtBetween(sellerId, status, startInstant, endInstant, page, size);
                totalElements = orderRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, status, startInstant, endInstant);
            } else {
                orders = orderRepository.findBySellerIdAndCreatedAtBetween(sellerId, startInstant, endInstant, page, size);
                totalElements = orderRepository.countBySellerIdAndCreatedAtBetween(sellerId, startInstant, endInstant);
            }
        } else if (status != null) {
            orders = orderRepository.findBySellerIdAndStatus(sellerId, status, page, size);
            totalElements = orderRepository.countBySellerIdAndStatus(sellerId, status);
        } else {
            orders = orderRepository.findBySellerId(sellerId, page, size);
            totalElements = orderRepository.countBySellerId(sellerId);
        }

        List<OrderResponse> content = orders.stream().map(mapper::toResponse).toList();
        return OrderPageResponse.of(content, page, size, totalElements);
    }
}

