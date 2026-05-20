package com.example.paymentservice.application.port.in;

import com.example.paymentservice.application.command.RefundPaymentCommand;
import com.example.paymentservice.domain.model.PaymentRefund;

public interface RefundPaymentUseCase {
    PaymentRefund refund(RefundPaymentCommand command);
}

