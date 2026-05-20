package com.example.orderservice.application.service;

import com.example.orderservice.application.dto.command.CreateOrderCommand;
import com.example.orderservice.application.dto.command.ItemCommand;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.mapper.OrderApplicationMapper;
import com.example.orderservice.application.port.input.CreateOrderUseCase;
import com.example.orderservice.application.saga.OrderSagaOrchestrator;
import com.example.orderservice.domain.event.OrderCreatedEvent;
import com.example.orderservice.domain.event.PlatformCouponAppliedEvent;
import com.example.orderservice.domain.exception.OrderDomainException;
import com.example.orderservice.domain.model.CheckoutSession;
import com.example.orderservice.domain.model.Order;
import com.example.orderservice.domain.model.OrderItem;
import com.example.orderservice.domain.model.OrderSagaState;
import com.example.orderservice.domain.model.SellerSession;
import com.example.orderservice.domain.model.valueobject.Money;
import com.example.orderservice.domain.port.output.EventPublisher;
import com.example.orderservice.domain.port.output.OrderRepository;
import com.example.orderservice.infrastructure.messaging.kafka.OrderNotificationPublisher;
import java.math.BigDecimal;
import com.example.orderservice.domain.model.enums.PaymentMethod;
import com.example.orderservice.infrastructure.client.PaymentFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Create Order Service - Implements CreateOrderUseCase
 * Uses Saga Pattern for distributed transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OrderApplicationMapper mapper;
    private final OrderNotificationPublisher notificationPublisher;
    private final ObjectMapper objectMapper;
    private final CheckoutSessionRedisService checkoutSessionRedisService;
    private final PaymentFeignClient paymentFeignClient;

    @Override
    @Transactional
    public List<OrderResponse> createOrder(CreateOrderCommand command) {
        log.info("[CreateOrder] === Creating orders for user: {}, checkoutSessionId: {} ===", 
                 command.getUserId(), command.getCheckoutSessionId());

        Object sessionData = checkoutSessionRedisService.getSession(command.getCheckoutSessionId().toString());
        if (sessionData == null) {
            throw new OrderDomainException("Checkout session not found or expired");
        }

        CheckoutSession checkoutSession;
        try {
            if (sessionData instanceof CheckoutSession) {
                // Already deserialized
                checkoutSession = (CheckoutSession) sessionData;
            } else if (sessionData instanceof java.util.Map) {
                // For old data without @class, serialize back to JSON and read with proper typing
                String json = objectMapper.writeValueAsString(sessionData);
                checkoutSession = objectMapper.readValue(json, CheckoutSession.class);
            } else {
                checkoutSession = objectMapper.convertValue(sessionData, CheckoutSession.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse checkout session data for sessionId {}: {}", command.getCheckoutSessionId(), e.getMessage(), e);
            throw new OrderDomainException("Failed to parse checkout session data");
        }

        if (!checkoutSession.getUserId().equals(command.getUserId())) {
            throw new OrderDomainException("User is not authorized to use this checkout session");
        }

        List<OrderResponse> responses = new ArrayList<>();

        // Pre-compute pro-rated platform voucher shares (option b: split by subtotal)
        UUID platformCouponId = checkoutSession.getVoucherId();
        Money platformDiscount = checkoutSession.getDiscountAmount() != null
                ? checkoutSession.getDiscountAmount()
                : Money.ZERO;
        List<Money> platformShares = computePlatformShares(
                checkoutSession.getSellerSessions(), platformDiscount);

        List<PlatformCouponAppliedEvent.OrderAllocation> allocations = new ArrayList<>();

        List<SellerSession> sellerSessions = checkoutSession.getSellerSessions();
        for (int i = 0; i < sellerSessions.size(); i++) {
            SellerSession sellerSession = sellerSessions.get(i);
            log.info("[CreateOrder] Processing order for seller: {}", sellerSession.getSellerId());

            final Order currentOrder = Order.create(
                    command.getUserId(),
                    sellerSession.getSellerId(),
                    checkoutSession.getShippingAddress(),
                    command.getPaymentMethod(),
                    sellerSession.getBuyerNote(),
                    sellerSession.getVoucherId() != null ? sellerSession.getVoucherId().toString() : null
            );

            // Add items
            sellerSession.getItems().forEach(checkoutItem -> {
                OrderItem item = OrderItem.create(
                        checkoutItem.getSkuId(),
                        checkoutItem.getProductId(),
                        checkoutItem.getProductName(),
                        checkoutItem.getSkuCode(),
                        checkoutItem.getImageUrl(),
                        checkoutItem.getUnitPrice(),
                        checkoutItem.getQuantity(),
                        checkoutItem.getVariantInfo()
                );
                currentOrder.addItem(item);
            });

            // Set shipping fee and seller-level discount
            if (sellerSession.getShippingFee() != null) {
                currentOrder.setShippingFee(sellerSession.getShippingFee());
            }
            if (sellerSession.getDiscountAmount() != null) {
                currentOrder.applyDiscount(sellerSession.getDiscountAmount());
            }

            // Apply pro-rated platform voucher share to this order
            Money share = platformShares.get(i);
            if (platformCouponId != null && share.isPositive()) {
                currentOrder.applyPlatformVoucher(platformCouponId, share);
            }

            // Save order
            Order savedOrder = orderRepository.save(currentOrder);
            log.info("[CreateOrder] Order saved: id={}, orderNumber={}, subtotal={}, sellerDiscount={}, platformShare={}, total={}",
                    savedOrder.getId().value(), savedOrder.getOrderNumber(),
                    savedOrder.getSubtotal(), savedOrder.getDiscountAmount(),
                    savedOrder.getPlatformVoucherShare(), savedOrder.getTotalAmount());

            if (platformCouponId != null && share.isPositive()) {
                allocations.add(PlatformCouponAppliedEvent.OrderAllocation.builder()
                        .orderId(savedOrder.getId().value())
                        .share(share.amount())
                        .build());
            }

            // Create Saga State
            OrderSagaState sagaState = OrderSagaState.create(savedOrder.getId());

            // Publish OrderCreatedEvent to Outbox
            OrderCreatedEvent event = buildOrderCreatedEvent(savedOrder);
            eventPublisher.publish(event);

            // Publish notification event
            notificationPublisher.publishOrderCreated(savedOrder, command.getBuyerEmail(), command.getBuyerName());

            // Start Saga orchestration
            sagaOrchestrator.startSaga(savedOrder, sagaState, checkoutSession.getCartId() != null);

            responses.add(mapper.toResponse(savedOrder));
        }

        // Emit a single platform-coupon event covering all orders, so discount-service
        // can mark the claim USED once and persist per-order usage history rows.
        if (platformCouponId != null && !allocations.isEmpty()) {
            eventPublisher.publish(PlatformCouponAppliedEvent.builder()
                    .couponId(platformCouponId)
                    .userId(command.getUserId())
                    .totalDiscount(platformDiscount.amount())
                    .allocations(allocations)
                    .build());
        }
        
        // Handle payment creation
        if (!responses.isEmpty()) {
            OrderResponse firstOrder = responses.get(0);
            if (command.getPaymentMethod() == PaymentMethod.VNPAY) {
                // Create VNPAY payment for the first order with total amount
                BigDecimal totalAmount = responses.stream()
                    .map(OrderResponse::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                var vnPayRequest = PaymentFeignClient.CreateVnPayPaymentRequest.builder()
                    .orderId(firstOrder.getId())
                        .userId(firstOrder.getUserId())
                    .amount(totalAmount)
                    .orderInfo("Thanh toán đơn hàng " + firstOrder.getOrderNumber())
                    .build();
                
                try {
                    var paymentRes = paymentFeignClient.createVnPayPayment(vnPayRequest);
                    if (paymentRes.isSuccess() && paymentRes.getData() != null) {
                        firstOrder.setPaymentUrl(paymentRes.getData().getPaymentUrl());
                    }
                } catch (Exception e) {
                    log.error("Failed to create VNPAY payment for order {}", firstOrder.getId(), e);
                    // Continue, payment can be retried
                }
            } else if (command.getPaymentMethod() == PaymentMethod.COD) {
                // Create COD payment for each order
                for (OrderResponse orderResponse : responses) {
                    var codRequest = PaymentFeignClient.CreateCodPaymentRequest.builder()
                        .orderId(orderResponse.getId())
                        .amount(orderResponse.getTotalAmount())
                            .userId(orderResponse.getUserId())
                        .shippingAddress(orderResponse.getShippingAddress())
                        .build();
                    
                    try {
                        paymentFeignClient.createCodPayment(codRequest);
                    } catch (Exception e) {
                        log.error("Failed to create COD payment for order {}", orderResponse.getId(), e);
                        // Continue, payment can be retried
                    }
                }
            }
        }
        
        // Remove checkout session after successful creation
        checkoutSessionRedisService.deleteSession(command.getCheckoutSessionId().toString());

        log.info("[CreateOrder] === Orders created successfully. Count: {} ===", responses.size());
        return responses;
    }

    /**
     * Pro-rate the platform discount across seller sessions by subtotal.
     * Each share is rounded HALF_UP to 2dp; the rounding remainder is absorbed
     * by the last seller so the sum equals the input platformDiscount exactly.
     */
    private List<Money> computePlatformShares(List<SellerSession> sellerSessions, Money platformDiscount) {
        int n = sellerSessions.size();
        List<Money> shares = new ArrayList<>(n);
        if (platformDiscount == null || platformDiscount.isZero() || n == 0) {
            for (int i = 0; i < n; i++) shares.add(Money.ZERO);
            return shares;
        }

        BigDecimal totalSubtotal = sellerSessions.stream()
                .map(s -> s.getTotalAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSubtotal.signum() == 0) {
            for (int i = 0; i < n; i++) shares.add(Money.ZERO);
            return shares;
        }

        Money accumulated = Money.ZERO;
        for (int i = 0; i < n; i++) {
            Money share;
            if (i == n - 1) {
                // Last seller absorbs the remainder so Σ shares == platformDiscount.
                share = platformDiscount.subtract(accumulated);
            } else {
                BigDecimal ratio = sellerSessions.get(i).getTotalAmount().amount()
                        .divide(totalSubtotal, 10, RoundingMode.HALF_UP);
                share = platformDiscount.multiply(ratio);
                accumulated = accumulated.add(share);
            }
            shares.add(share);
        }
        return shares;
    }

    private OrderCreatedEvent buildOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItemData> itemsData = order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemData.builder()
                        .skuId(item.getSkuId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getId().value())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .totalAmount(order.getTotalAmount().amount())
                .paymentMethod(order.getPaymentMethod())
                .items(itemsData)
                .build();
    }
}
