package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.command.CancelOrderCommand;
import com.example.orderservice.application.dto.command.ConfirmOrderCommand;
import com.example.orderservice.application.dto.command.ShipOrderCommand;
import com.example.orderservice.application.dto.response.OrderResponse;

import java.util.UUID;

/**
 * Input Port - Update Order Use Case
 */
public interface UpdateOrderUseCase {

    OrderResponse confirmOrder(ConfirmOrderCommand command);

    OrderResponse shipOrder(ShipOrderCommand command);

    OrderResponse deliverOrder(UUID orderId);

    OrderResponse completeOrder(UUID orderId, UUID userId);

    OrderResponse cancelOrder(CancelOrderCommand command);
}

