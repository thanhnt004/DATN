package com.example.paymentservice.application.port.out;

import com.example.paymentservice.domain.model.PaymentRefund;

import java.util.List;
import java.util.UUID;

public interface PaymentRefundRepositoryPort {
    PaymentRefund save(PaymentRefund refund);
    List<PaymentRefund> findByPaymentId(UUID paymentId);
}

