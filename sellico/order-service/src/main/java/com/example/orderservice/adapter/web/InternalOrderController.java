package com.example.orderservice.adapter.web;

import com.example.orderservice.application.dto.command.ProcessPaymentCommand;
import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.port.input.PaymentUseCase;
import com.example.orderservice.application.port.input.QueryOrderUseCase;
import com.example.orderservice.application.port.input.UpdateOrderUseCase;
import com.example.orderservice.domain.model.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for internal service communication
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalOrderController {

    private final QueryOrderUseCase queryOrderUseCase;
    private final UpdateOrderUseCase updateOrderUseCase;
    private final PaymentUseCase paymentUseCase;

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable("id") UUID id) {
        OrderResponse response = queryOrderUseCase.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@RequestBody List<UUID> orderIds) {
        List<OrderResponse> responses = queryOrderUseCase.getOrdersByIds(orderIds);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<ApiResponse<OrderPageResponse<OrderResponse>>> getUserOrders(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        OrderPageResponse<OrderResponse> response = queryOrderUseCase.getUserOrders(userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/orders/seller/{sellerId}")
    public ResponseEntity<ApiResponse<OrderPageResponse<OrderResponse>>> getSellerOrders(
            @PathVariable("sellerId") UUID sellerId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        OrderPageResponse<OrderResponse> response = queryOrderUseCase.getSellerOrders(sellerId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/orders/{id}/payment")
    public ResponseEntity<ApiResponse<OrderResponse>> processPayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProcessPaymentCommand command
    ) {
        command.setOrderId(id);
        OrderResponse response = paymentUseCase.processPayment(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsDelivered(@PathVariable("id") UUID id) {
        OrderResponse response = updateOrderUseCase.deliverOrder(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody com.example.orderservice.adapter.web.dto.ShipOrderRequestDto request
    ) {
        OrderResponse order = queryOrderUseCase.getOrderById(id);
        com.example.orderservice.application.dto.command.ShipOrderCommand command = com.example.orderservice.application.dto.command.ShipOrderCommand.builder()
                .orderId(id)
                .sellerId(order.getSellerId())
                // sellerId will be validated by the caller service (shipping-service)
                .shippingProvider(request.getShippingProvider())
                .trackingNumber(request.getTrackingNumber())
                .note(request.getNote())
                .build();
        OrderResponse response = updateOrderUseCase.shipOrder(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ═══════════════ Review-service validation endpoints ═══════════════

    /**
     * Check if an order belongs to a specific user
     */
    @GetMapping("/orders/{orderId}/belongs-to/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> checkOrderBelongsToUser(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("userId") UUID userId
    ) {
        OrderResponse order = queryOrderUseCase.getOrderById(orderId);
        boolean belongs = order.getUserId().equals(userId);
        return ResponseEntity.ok(ApiResponse.success(belongs));
    }

    /**
     * Check if an order contains a specific product
     */
    @GetMapping("/orders/{orderId}/has-product/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkOrderHasProduct(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("productId") UUID productId
    ) {
        OrderResponse order = queryOrderUseCase.getOrderById(orderId);
        boolean hasProduct = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> productId.equals(item.getProductId()));
        return ResponseEntity.ok(ApiResponse.success(hasProduct));
    }

    /**
     * Check if an order is in COMPLETED status
     */
    @GetMapping("/orders/{orderId}/is-completed")
    public ResponseEntity<ApiResponse<Boolean>> checkOrderIsCompleted(
            @PathVariable("orderId") UUID orderId
    ) {
        OrderResponse order = queryOrderUseCase.getOrderById(orderId);
        boolean completed = OrderStatus.COMPLETED == order.getStatus();
        return ResponseEntity.ok(ApiResponse.success(completed));
    }
}

