package com.example.paymentservice.application.service;

import com.example.paymentservice.application.command.RefundPaymentCommand;
import com.example.paymentservice.application.port.in.RefundPaymentUseCase;
import com.example.paymentservice.application.port.out.PaymentRefundRepositoryPort;
import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.domain.exception.PaymentBusinessException;
import com.example.paymentservice.domain.exception.PaymentErrorCode;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.PaymentRefund;
import com.example.paymentservice.domain.model.enums.PaymentStatus;
import com.example.paymentservice.domain.model.enums.RefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepositoryPort paymentRepo;
    private final PaymentRefundRepositoryPort refundRepo;

    @Override
    @Transactional
    public PaymentRefund refund(RefundPaymentCommand cmd) {
        Payment payment = paymentRepo.findById(cmd.getPaymentId())
                .orElseThrow(() -> new PaymentBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentBusinessException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
        }

        if (cmd.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentBusinessException(PaymentErrorCode.REFUND_AMOUNT_EXCEEDS);
        }

        Instant now = Instant.now();
        PaymentRefund refund = PaymentRefund.builder()
                .id(UUID.randomUUID())
                .paymentId(payment.getId())
                .amount(cmd.getAmount())
                .reason(cmd.getReason())
                .status(RefundStatus.COMPLETED) // For MVP, mark as completed directly
                .createdAt(now)
                .updatedAt(now)
                .build();

        refundRepo.save(refund);

        // Update payment status to REFUNDED
        payment.markRefunded();
        paymentRepo.save(payment);

        log.info("Refund processed for payment={}, amount={}", payment.getId(), cmd.getAmount());
        return refund;
    }
}

