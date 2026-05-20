package com.example.orderservice.application.saga;

import com.example.orderservice.application.dto.response.OrderSagaEvent;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderSagaState;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.example.orderservice.domain.port.output.CartPort;
import com.example.orderservice.domain.port.output.InventoryPort;
import com.example.orderservice.domain.port.output.OrderRepository;
import com.example.orderservice.infrastructure.persistence.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saga Orchestrator - Manages the order creation saga
 * Steps:
 * 1. Reserve Inventory
 * 2. Await Payment (for non-COD) or Confirm (for COD)
 * 3. Clear Cart
 * 4. Send Notification
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;
    private final InventoryPort inventoryPort;
    private final CartPort cartPort;
    private final OrderSagaEventPublisher eventPublisher;

    @Async
    public void startSaga(Order order, OrderSagaState sagaState, boolean clearCart) {
        log.info("[Saga] === Starting saga for order: {} (id={}) ===", order.getOrderNumber(), order.getId().value());
        log.info("[Saga] Order details: userId={}, sellerId={}, paymentMethod={}, totalAmount={}, items={}",
                order.getUserId(), order.getSellerId(), order.getPaymentMethod(),
                order.getTotalAmount(), order.getItems().size());

        try {
            // Emit initial processing event
            eventPublisher.publishEvent(OrderSagaEvent.processing(
                    order.getId().value(), 
                    order.getOrderNumber(), 
                    "Starting order creation saga"
            ));

            // Step 1: Reserve Inventory
            log.info("[Saga] Step 1: Reserve Inventory");
            eventPublisher.publishEvent(OrderSagaEvent.processing(
                    order.getId().value(), 
                    order.getOrderNumber(), 
                    "Reserving inventory"
            ));
            executeReserveInventory(order, sagaState);
            log.info("[Saga] Step 1 completed: Inventory reserved");

            // Step 2: Mark order as awaiting payment or ready for COD
            log.info("[Saga] Step 2: Update payment status (method={})", order.getPaymentMethod());
            eventPublisher.publishEvent(OrderSagaEvent.processing(
                    order.getId().value(), 
                    order.getOrderNumber(), 
                    "Processing payment"
            ));
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                sagaState.markPaymentCompleted();
                log.info("[Saga] COD order - stays PENDING, ready for seller confirmation");
            } else {
                sagaState.markPaymentPending();
                // Non-COD orders also stay PENDING - payment tracked via paymentStatus/paidAt
                log.info("[Saga] Non-COD order - payment marked as pending, order stays PENDING");
            }

            orderRepository.save(order);
            sagaStateRepository.save(sagaState);
            log.info("[Saga] Order and saga state saved. Order status={}, paymentStatus={}",
                    order.getStatus(), order.getPaymentStatus());

            // Step 3: Clear cart (async, best effort)
            log.info("[Saga] Step 3: Clear cart");
            eventPublisher.publishEvent(OrderSagaEvent.processing(
                    order.getId().value(), 
                    order.getOrderNumber(), 
                    "Clearing cart"
            ));
            executeClearCart(order, sagaState, clearCart);

            log.info("[Saga] === Saga completed successfully for order: {} ===", order.getOrderNumber());
            
            // Emit success event
            eventPublisher.publishEvent(OrderSagaEvent.completed(
                    order.getId().value(), 
                    order.getOrderNumber()
            ));

        } catch (Exception e) {
            log.error("[Saga] === Saga FAILED for order: {} === Error: {}", order.getOrderNumber(), e.getMessage(), e);
            
            // Emit failure event
            eventPublisher.publishEvent(OrderSagaEvent.failed(
                    order.getId().value(), 
                    order.getOrderNumber(), 
                    e.getMessage()
            ));
            
            handleSagaFailure(order, sagaState, e.getMessage());
        }
    }

    @Transactional
    public void executeReserveInventory(Order order, OrderSagaState sagaState) {
        log.info("Reserving inventory for order: {}", order.getOrderNumber());

        List<InventoryPort.ReservationItem> items = order.getItems().stream()
                .map(item -> new InventoryPort.ReservationItem(item.getSkuId(), item.getQuantity()))
                .toList();

        boolean reserved = inventoryPort.reserveStock(order.getId().value(), items);

        if (!reserved) {
            throw new RuntimeException("Failed to reserve inventory");
        }

        sagaState.markInventoryReserved();
        sagaStateRepository.save(sagaState);

        log.info("Inventory reserved for order: {}", order.getOrderNumber());
    }

    private void executeClearCart(Order order, OrderSagaState sagaState, boolean clearCart) {
        if (!clearCart) {
            log.info("Skipping cart clear for order {} because checkout was not from cart", order.getOrderNumber());
            return;
        }
        try {
            cartPort.clearSelectedItems(order.getUserId());
            sagaState.markCartCleared();
            sagaStateRepository.save(sagaState);
            log.info("Cart cleared for user: {}", order.getUserId());
        } catch (Exception e) {
            // Non-critical step, log and continue
            log.warn("Failed to clear cart for user: {}. Error: {}", order.getUserId(), e.getMessage());
        }
    }

    @Transactional
    public void handleSagaFailure(Order order, OrderSagaState sagaState, String error) {
        log.info("Handling saga failure for order: {}", order.getOrderNumber());

        sagaState.startCompensation(error);
        sagaStateRepository.save(sagaState);

        // Compensate: Release inventory if reserved
        if (sagaState.isInventoryReserved()) {
            try {
                inventoryPort.releaseReservation(order.getId().value());
                sagaState.markInventoryReleased();
            } catch (Exception e) {
                log.error("Failed to release inventory for order: {}", order.getOrderNumber(), e);
            }
        }

        // Mark order as cancelled
        order.cancel(error);
        orderRepository.save(order);

        sagaState.markCompensated();
        sagaStateRepository.save(sagaState);

        log.info("Saga compensation completed for order: {}", order.getOrderNumber());
    }

    @Transactional
    public void completeSaga(Order order, OrderSagaState sagaState) {
        sagaState.complete();
        sagaStateRepository.save(sagaState);
        log.info("Saga completed for order: {}", order.getOrderNumber());
    }
}

