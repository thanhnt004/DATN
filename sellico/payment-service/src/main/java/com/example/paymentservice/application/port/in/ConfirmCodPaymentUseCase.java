package com.example.paymentservice.application.port.in;

import com.example.paymentservice.application.command.ConfirmCodPaymentCommand;
import com.example.paymentservice.domain.model.Payment;

import java.util.UUID;

public interface ConfirmCodPaymentUseCase {
    /**
     * Confirms that COD cash has been collected by delivery agent.
     * Transitions: COD_PENDING → COMPLETED, publishes payment.completed event.
     */
    Payment confirmCodPayment(ConfirmCodPaymentCommand command);

    /**
     * Cancels a COD payment (order cancelled before delivery).
     * Transitions: COD_PENDING → FAILED.
     */
    Payment cancelCodPayment(UUID paymentId, String reason);
}

