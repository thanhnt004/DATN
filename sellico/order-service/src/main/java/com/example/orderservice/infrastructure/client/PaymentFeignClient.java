package com.example.orderservice.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import response.ApiResponse;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {

    @PostMapping("/internal/v1/payments/vnpay/create")
    ApiResponse<VnPayCreateResponse> createVnPayPayment(@RequestBody CreateVnPayPaymentRequest request);

    @PostMapping("/internal/v1/payments/cod/create")
    ApiResponse<PaymentResponse> createCodPayment(@RequestBody CreateCodPaymentRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CreateVnPayPaymentRequest {
        private UUID orderId;
        private UUID userId;
        private BigDecimal amount;
        private String orderInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CreateCodPaymentRequest {
        private UUID orderId;
        private BigDecimal amount;
        private UUID userId;
        private String shippingAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class VnPayCreateResponse {
        private String paymentUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PaymentResponse {
        private UUID id;
        private UUID orderId;
        private UUID userId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String status;
        private String vnpayTxnRef;
        private String vnpayTransactionNo;
        private String bankCode;
        private String cardType;
        private String payDate;
        private String failureReason;
        private String createdAt;
        private String updatedAt;
    }
}