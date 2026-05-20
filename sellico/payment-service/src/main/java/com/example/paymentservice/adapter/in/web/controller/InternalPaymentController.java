package com.example.paymentservice.adapter.in.web.controller;

import com.example.paymentservice.adapter.in.web.request.CreateCodPaymentRequest;
import com.example.paymentservice.adapter.in.web.request.CreateVnPayPaymentRequest;
import com.example.paymentservice.adapter.in.web.response.PaymentResponse;
import com.example.paymentservice.adapter.in.web.response.VnPayCreateResponse;
import com.example.paymentservice.application.command.ConfirmCodPaymentCommand;
import com.example.paymentservice.application.command.CreateCodPaymentCommand;
import com.example.paymentservice.application.command.CreateVnPayPaymentCommand;
import com.example.paymentservice.application.port.in.ConfirmCodPaymentUseCase;
import com.example.paymentservice.application.port.in.CreateCodPaymentUseCase;
import com.example.paymentservice.application.port.in.CreateVnPayPaymentUseCase;
import com.example.paymentservice.application.port.in.QueryPaymentUseCase;
import com.example.paymentservice.domain.model.Payment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

/**
 * Internal endpoints (no auth, called by other microservices):
 * - GET  /internal/v1/payments/order/{orderId}         → get payment by order
 * - POST /internal/v1/payments/cod/create               → create COD payment (from order-service)
 * - POST /internal/v1/payments/cod/{id}/confirm         → confirm COD cash collected
 * - POST /internal/v1/payments/cod/{id}/cancel          → cancel COD payment
 */
@RestController
@RequestMapping("/internal/v1/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final QueryPaymentUseCase queryPaymentUseCase;
    private final CreateCodPaymentUseCase createCodPaymentUseCase;
    private final ConfirmCodPaymentUseCase confirmCodPaymentUseCase;
    private final CreateVnPayPaymentUseCase createVnPayPaymentUseCase;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable("orderId") UUID orderId) {
        Payment payment = queryPaymentUseCase.getByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    /**
     * POST /internal/v1/payments/vnpay/create — order-service creates VNPAY payment during checkout
     */
    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<VnPayCreateResponse>> createVnPayPaymentInternal(
            @Valid @RequestBody CreateVnPayPaymentRequest request) {
        CreateVnPayPaymentCommand command = CreateVnPayPaymentCommand.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .orderInfo(request.getOrderInfo())
                .ipAddress("127.0.0.1") // Default for internal calls
                .build();

        String paymentUrl = createVnPayPaymentUseCase.createPaymentUrl(command);
        return ResponseEntity.ok(ApiResponse.success(
                VnPayCreateResponse.builder().paymentUrl(paymentUrl).build()
        ));
    }

    /**
     * POST /internal/v1/payments/cod/create — order-service creates COD payment during checkout
     */
    @PostMapping("/cod/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCodPaymentInternal(
            @Valid @RequestBody CreateCodPaymentRequest request) {
        CreateCodPaymentCommand command = CreateCodPaymentCommand.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId()) // Internal call, userId may come from request or be null
                .amount(request.getAmount())
                .shippingAddress(request.getShippingAddress())
                .build();

        Payment payment = createCodPaymentUseCase.createCodPayment(command);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    /**
     * POST /internal/v1/payments/cod/{id}/confirm — delivery-service confirms cash collected
     */
    @PostMapping("/cod/{id}/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmCodInternal(@PathVariable("id") UUID id) {
        ConfirmCodPaymentCommand command = ConfirmCodPaymentCommand.builder()
                .paymentId(id)
                .confirmedBy(null)
                .note("Internal confirm")
                .build();
        Payment payment = confirmCodPaymentUseCase.confirmCodPayment(command);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    /**
     * POST /internal/v1/payments/cod/{id}/cancel — order-service cancels COD on order cancellation
     */
    @PostMapping("/cod/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelCodInternal(
            @PathVariable("id") UUID id,
            @RequestParam(value = "reason", required = false) String reason) {
        Payment payment = confirmCodPaymentUseCase.cancelCodPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus().name())
                .vnpayTxnRef(p.getVnpayTxnRef())
                .vnpayTransactionNo(p.getVnpayTransactionNo())
                .bankCode(p.getBankCode())
                .cardType(p.getCardType())
                .payDate(p.getPayDate())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

