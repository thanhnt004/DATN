package com.example.paymentservice.application.service;

import com.example.paymentservice.application.port.in.QueryPaymentUseCase;
import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.domain.exception.PaymentBusinessException;
import com.example.paymentservice.domain.exception.PaymentErrorCode;
import com.example.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService implements QueryPaymentUseCase {

    private final PaymentRepositoryPort paymentRepo;

    @Override
    @Transactional(readOnly = true)
    public Payment getById(UUID paymentId) {
        return paymentRepo.findById(paymentId)
                .orElseThrow(() -> new PaymentBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getByOrderId(UUID orderId) {
        return paymentRepo.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }
}

