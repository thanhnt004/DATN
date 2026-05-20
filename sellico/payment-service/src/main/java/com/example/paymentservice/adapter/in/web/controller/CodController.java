package com.example.paymentservice.adapter.in.web.controller;

import com.example.paymentservice.adapter.in.web.request.CancelCodRequest;
import com.example.paymentservice.adapter.in.web.request.ConfirmCodRequest;
import com.example.paymentservice.adapter.in.web.response.PaymentResponse;
import com.example.paymentservice.application.command.ConfirmCodPaymentCommand;
import com.example.paymentservice.application.port.in.ConfirmCodPaymentUseCase;
import com.example.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.net.URI;
import java.util.UUID;

/**
 * COD (Cash On Delivery) payment endpoints:
 * - POST /api/v1/payments/cod/{id}/confirm      (seller/admin)  → confirm cash collected
 * - POST /api/v1/payments/cod/{id}/cancel       (buyer/seller)  → cancel COD payment
 */
@RestController
@RequestMapping("/api/v1/payments/cod")
@RequiredArgsConstructor
@Slf4j
public class CodController {

    private final ConfirmCodPaymentUseCase confirmCodPaymentUseCase;

    /**
     * POST /api/v1/payments/cod/{id}/confirm — Seller/delivery confirms cash collected
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmCodPayment(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ConfirmCodRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID confirmedBy = UUID.fromString(jwt.getSubject());

        ConfirmCodPaymentCommand command = ConfirmCodPaymentCommand.builder()
                .paymentId(id)
                .confirmedBy(confirmedBy)
                .note(request != null ? request.getNote() : null)
                .build();

        Payment payment = confirmCodPaymentUseCase.confirmCodPayment(command);
        return ResponseEntity.ok(ApiResponse.success(toResponse(payment)));
    }

    /**
     * POST /api/v1/payments/cod/{id}/cancel — Cancel COD payment (before delivery)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelCodPayment(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) CancelCodRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
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

