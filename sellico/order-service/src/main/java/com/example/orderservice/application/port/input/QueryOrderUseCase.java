package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.domain.model.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input Port - Query Order Use Case
 */
public interface QueryOrderUseCase {

    OrderResponse getOrderById(UUID orderId);

    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getOrdersByIds(List<UUID> orderIds);
    OrderPageResponse<OrderResponse> getUserOrders(UUID userId, OrderStatus status, int page, int size);

    OrderPageResponse<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, int page, int size);

    OrderPageResponse<OrderResponse> getSellerOrders(UUID sellerId, OrderStatus status, String orderNumber, LocalDate startDate, LocalDate endDate, int page, int size);
}

