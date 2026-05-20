package com.example.paymentservice.application.port.in;

import com.example.paymentservice.application.command.CreateVnPayPaymentCommand;

public interface CreateVnPayPaymentUseCase {
    /**
     * Creates a VNPay payment URL for an order.
     * @return the VNPay redirect URL
     */
    String createPaymentUrl(CreateVnPayPaymentCommand command);
}

