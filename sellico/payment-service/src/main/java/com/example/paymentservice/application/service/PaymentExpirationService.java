package com.example.paymentservice.application.service;

import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Auto-expires PENDING payments older than 15 minutes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpirationService {

    private final PaymentRepositoryPort paymentRepo;

    @Scheduled(fixedDelay = 60_000) // every minute
    @Transactional
    public void expireStalePendingPayments() {
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        List<Payment> stalePayments = paymentRepo.findPendingPaymentsCreatedBefore(cutoff);

        for (Payment payment : stalePayments) {
            payment.markExpired();
            paymentRepo.save(payment);
            log.info("Auto-expired payment: id={}, orderId={}", payment.getId(), payment.getOrderId());
        }
    }
}

