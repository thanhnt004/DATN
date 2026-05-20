package com.example.paymentservice.adapter.out.persistence.mapper;

import com.example.paymentservice.adapter.out.persistence.entity.PaymentJpaEntity;
import com.example.paymentservice.adapter.out.persistence.entity.PaymentRefundJpaEntity;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.PaymentRefund;
import com.example.paymentservice.domain.model.enums.PaymentStatus;
import com.example.paymentservice.domain.model.enums.RefundStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentJpaEntity e) {
        if (e == null) return null;
        return Payment.builder()
                .id(e.getId())
                .orderId(e.getOrderId())
                .userId(e.getUserId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .paymentMethod(e.getPaymentMethod())
                .status(PaymentStatus.valueOf(e.getStatus()))
                .vnpayTxnRef(e.getVnpayTxnRef())
                .vnpayTransactionNo(e.getVnpayTransactionNo())
                .bankCode(e.getBankCode())
                .cardType(e.getCardType())
                .payDate(e.getPayDate())
                .failureReason(e.getFailureReason())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public PaymentJpaEntity toEntity(Payment d) {
        if (d == null) return null;
        return PaymentJpaEntity.builder()
                .id(d.getId())
                .orderId(d.getOrderId())
                .userId(d.getUserId())
                .amount(d.getAmount())
                .currency(d.getCurrency())
                .paymentMethod(d.getPaymentMethod())
                .status(d.getStatus().name())
                .vnpayTxnRef(d.getVnpayTxnRef())
                .vnpayTransactionNo(d.getVnpayTransactionNo())
                .bankCode(d.getBankCode())
                .cardType(d.getCardType())
                .payDate(d.getPayDate())
                .failureReason(d.getFailureReason())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public PaymentRefund toDomain(PaymentRefundJpaEntity e) {
        if (e == null) return null;
        return PaymentRefund.builder()
                .id(e.getId())
                .paymentId(e.getPaymentId())
                .amount(e.getAmount())
                .reason(e.getReason())
                .status(RefundStatus.valueOf(e.getStatus()))
                .vnpayTxnRef(e.getVnpayTxnRef())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public PaymentRefundJpaEntity toEntity(PaymentRefund d) {
        if (d == null) return null;
        return PaymentRefundJpaEntity.builder()
                .id(d.getId())
                .paymentId(d.getPaymentId())
                .amount(d.getAmount())
                .reason(d.getReason())
                .status(d.getStatus().name())
                .vnpayTxnRef(d.getVnpayTxnRef())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

