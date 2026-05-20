package com.example.paymentservice.application.port.in;

import com.example.paymentservice.application.command.CreateCodPaymentCommand;
import com.example.paymentservice.domain.model.Payment;

public interface CreateCodPaymentUseCase {
    /**
     * Creates a COD payment record for an order.
     * The payment stays in COD_PENDING status until delivery confirmation.
     */
    Payment createCodPayment(CreateCodPaymentCommand command);
}

