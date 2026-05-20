package com.example.paymentservice.domain.model.enums;

public enum PaymentStatus {
    PENDING,          // Online payment pending (VNPay)
    COMPLETED,        // Payment completed
    FAILED,           // Payment failed
    REFUNDED,         // Payment refunded
    EXPIRED,          // Payment expired (timeout)
    COD_PENDING,      // COD: Awaiting delivery & cash collection
    COD_CONFIRMED     // COD: Cash collected by delivery agent
}

