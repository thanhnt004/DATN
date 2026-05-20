package com.example.orderservice.adapter.web;

import com.example.orderservice.adapter.web.dto.CreateOrderRequestDto;
import com.example.orderservice.adapter.web.dto.CancelOrderRequestDto;
import com.example.orderservice.adapter.web.mapper.OrderWebMapper;
import com.example.orderservice.application.dto.response.OrderPageResponse;
import com.example.orderservice.application.dto.response.OrderResponse;
import com.example.orderservice.application.port.input.CreateOrderUseCase;
import com.example.orderservice.application.port.input.QueryOrderUseCase;
import com.example.orderservice.application.port.input.UpdateOrderUseCase;
import com.example.orderservice.domain.model.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for buyer order operations
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final QueryOrderUseCase queryOrderUseCase;
    private final UpdateOrderUseCase updateOrderUseCase;
    private final OrderWebMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> createOrder(
            @Valid @RequestBody CreateOrderRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("preferred_username");
        var command = mapper.toCommand(request, userId, email, name);
        List<OrderResponse> response = createOrderUseCase.createOrder(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<OrderPageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        OrderPageResponse<OrderResponse> response = queryOrderUseCase.getUserOrders(userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable("id") UUID id) {
        OrderResponse response = queryOrderUseCase.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable("orderNumber") String orderNumber) {
        OrderResponse response = queryOrderUseCase.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CancelOrderRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var command = mapper.toCancelCommand(id, userId, request, false);
        OrderResponse response = updateOrderUseCase.cancelOrder(command);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}

