package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.command.CreateOrderCommand;
import com.example.orderservice.application.dto.response.OrderResponse;

import java.util.List;

/**
 * Input Port - Create Order Use Case
 */
public interface CreateOrderUseCase {

    List<OrderResponse> createOrder(CreateOrderCommand command);
}

