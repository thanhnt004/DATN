package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.command.ProcessPaymentCommand;
import com.example.orderservice.application.dto.response.OrderResponse;

/**
 * Input Port - Payment Use Case
 */
public interface PaymentUseCase {

    OrderResponse processPayment(ProcessPaymentCommand command);
}

