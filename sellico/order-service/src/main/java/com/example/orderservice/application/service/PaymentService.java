package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.command.ProcessPaymentCommand;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.exception.OrderNotFoundException;
import com.example.orderservice.application.exception.PaymentException;
import com.example.orderservice.application.mapper.OrderApplicationMapper;
import com.example.orderservice.application.port.input.PaymentUseCase;
import com.example.orderservice.domain.event.OrderPaidEvent;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.example.orderservice.domain.model.valueobject.OrderId;
import com.example.orderservice.domain.port.output.EventPublisher;
import com.example.orderservice.domain.port.output.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Payment Service - Implements PaymentUseCase
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements PaymentUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final OrderApplicationMapper mapper;

    @Override
    @Transactional
    public OrderResponse processPayment(ProcessPaymentCommand command) {
        Order order = orderRepository.findById(new OrderId(command.getOrderId()))
                .orElseThrow(() -> new OrderNotFoundException(command.getOrderId()));

        // Validate payment amount
        if (command.getAmount().compareTo(order.getTotalAmount().amount()) != 0) {
            throw new PaymentException("Payment amount mismatch");
        }

        // COD orders should not be marked paid via this flow
        // COD payment is tracked separately — paidAt is set only when cash is collected
        if (order.isCOD()) {
            log.info("Skipping markPaid for COD order: {}", order.getOrderNumber());
            return mapper.toResponse(order);
        }

        // Update payment status for online payments
        order.markPaid(Instant.now());
        order = orderRepository.save(order);

        // Publish event
        OrderPaidEvent event = OrderPaidEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .amount(command.getAmount())
                .transactionId(command.getTransactionId())
                .build();
        eventPublisher.publish(event);

        log.info("Payment processed for order: {}", order.getOrderNumber());
        return mapper.toResponse(order);
    }
}

