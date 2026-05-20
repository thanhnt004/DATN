package com.example.paymentservice.application.service;

import com.example.paymentservice.application.command.ConfirmCodPaymentCommand;
import com.example.paymentservice.application.command.CreateCodPaymentCommand;
import com.example.paymentservice.application.port.in.ConfirmCodPaymentUseCase;
import com.example.paymentservice.application.port.in.CreateCodPaymentUseCase;
import com.example.paymentservice.application.port.out.EventPublisherPort;
import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.domain.event.PaymentCompletedEvent;
import com.example.paymentservice.domain.exception.PaymentBusinessException;
import com.example.paymentservice.domain.exception.PaymentErrorCode;
import com.example.paymentservice.domain.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodPaymentService implements CreateCodPaymentUseCase, ConfirmCodPaymentUseCase {

    private final PaymentRepositoryPort paymentRepo;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.cod.max-amount:50000000}")
    private BigDecimal codMaxAmount;

    // =====================================================
    // Create COD Payment
    // =====================================================

    @Override
    @Transactional
    public Payment createCodPayment(CreateCodPaymentCommand cmd) {
        // 1. Check if order already has a payment
        if (paymentRepo.existsPendingOrCompletedByOrderId(cmd.getOrderId())) {
            throw new PaymentBusinessException(PaymentErrorCode.ORDER_ALREADY_HAS_PAYMENT);
        }

        // 2. Validate COD amount limit
        if (cmd.getAmount().compareTo(codMaxAmount) > 0) {
            throw new PaymentBusinessException(PaymentErrorCode.COD_AMOUNT_EXCEEDS_LIMIT);
        }

        // 3. Create COD payment record
        Payment payment = Payment.createCod(
                cmd.getOrderId(),
                cmd.getUserId(),
                cmd.getAmount(),
                cmd.getShippingAddress()
        );

        Payment saved = paymentRepo.save(payment);
        log.info("COD payment created: id={}, orderId={}, amount={}",
                saved.getId(), saved.getOrderId(), saved.getAmount());

        // COD payment is "pay later" — do NOT publish payment.completed here.
        // It will be published when cash is actually collected (confirmCodPayment).

        return saved;
    }

    // =====================================================
    // Confirm COD (cash collected)
    // =====================================================

    @Override
    @Transactional
    public Payment confirmCodPayment(ConfirmCodPaymentCommand cmd) {
        Payment payment = findPaymentOrThrow(cmd.getPaymentId());

        if (!payment.isCod()) {
            throw new PaymentBusinessException(PaymentErrorCode.INVALID_PAYMENT_METHOD);
        }

        if (!payment.isCodPending()) {
            throw new PaymentBusinessException(PaymentErrorCode.COD_NOT_PENDING);
        }

        // Mark as completed (cash collected)
        payment.markCodCompleted();
        Payment saved = paymentRepo.save(payment);

        // Publish payment.completed event now that cash is collected
        publishPaymentCompletedEvent(saved);

        log.info("COD payment confirmed: id={}, orderId={}, confirmedBy={}",
                saved.getId(), saved.getOrderId(), cmd.getConfirmedBy());

        return saved;
    }

    // =====================================================
    // Cancel COD (order cancelled)
    // =====================================================

    @Override
    @Transactional
    public Payment cancelCodPayment(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);

        if (!payment.isCod()) {
            throw new PaymentBusinessException(PaymentErrorCode.INVALID_PAYMENT_METHOD);
        }

        if (!payment.isCodPending()) {
            throw new PaymentBusinessException(PaymentErrorCode.COD_NOT_PENDING);
        }

        payment.markCodCancelled(reason);
        Payment saved = paymentRepo.save(payment);

        log.info("COD payment cancelled: id={}, orderId={}, reason={}",
                saved.getId(), saved.getOrderId(), reason);

        return saved;
    }

    // =====================================================
    // Helpers
    // =====================================================

    private Payment findPaymentOrThrow(UUID id) {
        return paymentRepo.findById(id)
                .orElseThrow(() -> new PaymentBusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(payment.getOrderId())
                    .paymentMethod(payment.getPaymentMethod())
                    .amount(payment.getAmount())
                    .transactionId(payment.getId().toString())
                    .paymentGateway("COD")
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            eventPublisher.publishPaymentCompleted(payment.getOrderId().toString(), payload);
        } catch (Exception e) {
            log.error("Failed to publish COD payment event for payment={}", payment.getId(), e);
        }
    }
}

