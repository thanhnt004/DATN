package com.example.paymentservice.application.service;

import com.example.paymentservice.application.command.CreateVnPayPaymentCommand;
import com.example.paymentservice.application.port.in.CreateVnPayPaymentUseCase;
import com.example.paymentservice.application.port.in.HandleVnPayCallbackUseCase;
import com.example.paymentservice.application.port.out.EventPublisherPort;
import com.example.paymentservice.application.port.out.PaymentRepositoryPort;
import com.example.paymentservice.application.port.out.VnPayGatewayPort;
import com.example.paymentservice.domain.event.PaymentCompletedEvent;
import com.example.paymentservice.domain.exception.PaymentBusinessException;
import com.example.paymentservice.domain.exception.PaymentErrorCode;
import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.domain.model.enums.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayPaymentService implements CreateVnPayPaymentUseCase, HandleVnPayCallbackUseCase {

    private final PaymentRepositoryPort paymentRepo;
    private final VnPayGatewayPort vnPayGateway;
    private final EventPublisherPort eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public String createPaymentUrl(CreateVnPayPaymentCommand cmd) {
        // Check if order already has pending/completed payment
        if (paymentRepo.existsPendingOrCompletedByOrderId(cmd.getOrderId())) {
            throw new PaymentBusinessException(PaymentErrorCode.ORDER_ALREADY_HAS_PAYMENT);
        }

        // Generate unique txn ref
        String vnpTxnRef = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // Create pending payment record
        Payment payment = Payment.createPending(cmd.getOrderId(), cmd.getUserId(), cmd.getAmount(), vnpTxnRef);
        paymentRepo.save(payment);

        // Build VNPay URL
        String orderInfo = cmd.getOrderInfo() != null ? cmd.getOrderInfo()
                : "Thanh toan don hang " + cmd.getOrderId();
        return vnPayGateway.buildPaymentUrl(vnpTxnRef, cmd.getAmount(), orderInfo, cmd.getIpAddress());
    }

    /**
     * Handle VNPay IPN (server-to-server callback).
     * Must be idempotent - VNPay may call this multiple times.
     */
    @Override
    @Transactional
    public IpnResult handleIpn(Map<String, String> params) {
        // 1. Verify signature
        if (!vnPayGateway.verifySignature(params)) {
            log.warn("Invalid VNPay IPN signature");
            return new IpnResult("97", "Invalid Checksum");
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpBankCode = params.get("vnp_BankCode");
        String vnpCardType = params.get("vnp_CardType");
        String vnpPayDate = params.get("vnp_PayDate");

        // 2. Find payment
        Payment payment = paymentRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
        if (payment == null) {
            log.warn("IPN: payment not found for txnRef={}", vnpTxnRef);
            return new IpnResult("01", "Order Not Found");
        }

        // 3. Idempotency check
        if (payment.isCompleted()) {
            log.info("IPN: payment already completed for txnRef={}", vnpTxnRef);
            return new IpnResult("00", "Confirm Success");
        }

        if (!payment.isPending()) {
            log.warn("IPN: payment not in PENDING status for txnRef={}", vnpTxnRef);
            return new IpnResult("02", "Order Already Confirmed");
        }

        // 4. Process
        if ("00".equals(vnpResponseCode)) {
            payment.markCompleted(vnpTransactionNo, vnpBankCode, vnpCardType, vnpPayDate);
            paymentRepo.save(payment);

            // 5. Publish payment.completed event via outbox
            publishPaymentCompletedEvent(payment);
            log.info("IPN: Payment completed for order={}, txnRef={}", payment.getOrderId(), vnpTxnRef);
            return new IpnResult("00", "Confirm Success");
        } else {
            payment.markFailed("VNPay response code: " + vnpResponseCode);
            paymentRepo.save(payment);
            log.info("IPN: Payment failed for txnRef={}, code={}", vnpTxnRef, vnpResponseCode);
            return new IpnResult("00", "Confirm Success");
        }
    }

    /**
     * Handle VNPay Return URL (user redirect back to frontend).
     */
    @Override
    @Transactional(readOnly = true)
    public ReturnResult handleReturn(Map<String, String> params) {
        if (!vnPayGateway.verifySignature(params)) {
            return new ReturnResult(false, "Invalid signature", null);
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");

        Payment payment = paymentRepo.findByVnpayTxnRef(vnpTxnRef).orElse(null);
        if (payment == null) {
            return new ReturnResult(false, "Payment not found", null);
        }

        boolean success = "00".equals(vnpResponseCode);
        return new ReturnResult(success,
                success ? "Payment successful" : "Payment failed (code: " + vnpResponseCode + ")",
                payment.getOrderId().toString());
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(payment.getOrderId())
                    .paymentMethod(payment.getPaymentMethod())
                    .amount(payment.getAmount())
                    .transactionId(payment.getVnpayTransactionNo())
                    .paymentGateway("VNPAY")
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            eventPublisher.publishPaymentCompleted(payment.getOrderId().toString(), payload);
        } catch (Exception e) {
            log.error("Failed to publish payment completed event for payment={}", payment.getId(), e);
        }
    }
}

