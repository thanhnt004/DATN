package com.example.paymentservice.domain.exception;

import response.BaseErrorCode;

public enum PaymentErrorCode implements BaseErrorCode {
    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "Payment not found", 404),
    PAYMENT_ALREADY_COMPLETED("PAYMENT_ALREADY_COMPLETED", "Payment has already been completed", 409),
    PAYMENT_NOT_COMPLETABLE("PAYMENT_NOT_COMPLETABLE", "Payment cannot be completed in current status", 400),
    PAYMENT_NOT_REFUNDABLE("PAYMENT_NOT_REFUNDABLE", "Payment cannot be refunded in current status", 400),
    REFUND_AMOUNT_EXCEEDS("REFUND_AMOUNT_EXCEEDS", "Refund amount exceeds payment amount", 400),
    INVALID_VNPAY_SIGNATURE("INVALID_VNPAY_SIGNATURE", "Invalid VNPay signature", 400),
    VNPAY_PAYMENT_FAILED("VNPAY_PAYMENT_FAILED", "VNPay payment failed", 400),
    ORDER_ALREADY_HAS_PAYMENT("ORDER_ALREADY_HAS_PAYMENT", "Order already has a pending or completed payment", 409),
    COD_NOT_PENDING("COD_NOT_PENDING", "COD payment is not in pending status", 400),
    COD_AMOUNT_EXCEEDS_LIMIT("COD_AMOUNT_EXCEEDS_LIMIT", "COD amount exceeds the allowed limit", 400),
    INVALID_PAYMENT_METHOD("INVALID_PAYMENT_METHOD", "Invalid payment method", 400),
    VNPAY_REFUND_FAILED("VNPAY_REFUND_FAILED", "VNPay refund request failed", 500),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int statusCode;

    PaymentErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public int getStatusCode() { return statusCode; }
}

