package com.example.paymentservice.adapter.in.web.controller;

import com.example.paymentservice.adapter.in.web.request.RefundRequest;
import com.example.paymentservice.adapter.in.web.response.PaymentResponse;
import com.example.paymentservice.adapter.in.web.response.RefundResponse;
import com.example.paymentservice.application.command.RefundPaymentCommand;
import com.example.paymentservice.application.port.in.QueryPaymentUseCase;
import com.example.paymentservice.application.port.in.RefundPaymentUseCase;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.PaymentRefund;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

/**
 * Authenticated payment endpoints:
 * - GET  /api/v1/payments/{id}           → get payment details
 * - GET  /api/v1/payments/order/{orderId} → get payment by order
 * - POST /api/v1/payments/{id}/refund    → request refund
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final QueryPaymentUseCase queryPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable("id") UUID id) {
        Payment payment = queryPaymentUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(@PathVariable("orderId") UUID orderId) {
        Payment payment = queryPaymentUseCase.getByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        RefundPaymentCommand command = RefundPaymentCommand.builder()
                .paymentId(id)
                .amount(request.getAmount())
                .reason(request.getReason())
                .userId(userId)
                .build();

        PaymentRefund refund = refundPaymentUseCase.refund(command);

        RefundResponse response = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus().name())
                .createdAt(refund.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
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

