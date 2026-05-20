package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.command.CancelOrderCommand;
import com.example.orderservice.application.dto.command.ConfirmOrderCommand;
import com.example.orderservice.application.dto.command.ShipOrderCommand;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.exception.OrderNotFoundException;
import com.example.orderservice.application.exception.UnauthorizedException;
import com.example.orderservice.application.mapper.OrderApplicationMapper;
import com.example.orderservice.application.port.input.UpdateOrderUseCase;
import com.example.orderservice.domain.event.*;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.port.output.EventPublisher;
import com.example.orderservice.domain.port.output.InventoryPort;
import com.example.orderservice.domain.port.output.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Update Order Service - Implements UpdateOrderUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderService implements UpdateOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final InventoryPort inventoryPort;
    private final OrderApplicationMapper mapper;

    @Override
    @Transactional
    public OrderResponse confirmOrder(ConfirmOrderCommand command) {
        Order order = getOrderForSeller(command.getOrderId(), command.getSellerId());

        order.confirm();
        order = orderRepository.save(order);

        // Confirm inventory reservation
        inventoryPort.confirmReservation(command.getOrderId());

        // Publish event
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .build();
        eventPublisher.publish(event);

        log.info("Order confirmed: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse shipOrder(ShipOrderCommand command) {
        Order order = getOrderForSeller(command.getOrderId(), command.getSellerId());

        order.ship(command.getShippingProvider(), command.getTrackingNumber());
        order = orderRepository.save(order);

        // Publish event
        OrderShippedEvent event = OrderShippedEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .shippingProvider(command.getShippingProvider())
                .trackingNumber(command.getTrackingNumber())
                .build();
        eventPublisher.publish(event);

        log.info("Order shipped: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse deliverOrder(UUID orderId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.deliver();
        order = orderRepository.save(order);

        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .build();
        eventPublisher.publish(event);

        log.info("Order delivered: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to complete this order");
        }

        order.complete();
        order = orderRepository.save(order);

        // Publish event
        List<OrderCompletedEvent.OrderItemInfo> itemInfos = order.getItems().stream()
                .map(item -> OrderCompletedEvent.OrderItemInfo.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .items(itemInfos)
                .build();
        eventPublisher.publish(event);

        log.info("Order completed: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(CancelOrderCommand command) {
        Order order = orderRepository.findById(new OrderId(command.getOrderId()))
                .orElseThrow(() -> new OrderNotFoundException(command.getOrderId()));

        // Validate ownership
        if (command.isSeller()) {
            if (!order.getSellerId().equals(command.getUserId())) {
                throw new UnauthorizedException("You are not the seller of this order");
            }
        } else {
            if (!order.getUserId().equals(command.getUserId())) {
                throw new UnauthorizedException("You are not the owner of this order");
            }
        }

        order.cancel(command.getReason());
        order = orderRepository.save(order);

        // Release inventory reservation
        inventoryPort.releaseReservation(command.getOrderId());

        // Publish event
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .reason(command.getReason())
                .inventoryReserved(true)
                .paymentProcessed(order.isPaid())
                .build();
        eventPublisher.publish(event);

        log.info("Order cancelled: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }

    private Order getOrderForSeller(UUID orderId, UUID sellerId) {
        Order order = orderRepository.findById(new OrderId(orderId))
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getSellerId().equals(sellerId)) {
            throw new UnauthorizedException("You are not the seller of this order");
        }
        return order;
    }
}

