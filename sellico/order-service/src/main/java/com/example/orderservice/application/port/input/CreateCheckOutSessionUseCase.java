package com.example.orderservice.application.port.input;

import com.example.orderservice.application.dto.command.CreateCheckoutSessionCommand;
import com.example.orderservice.application.dto.response.CheckoutSessionResponse;

public interface CreateCheckOutSessionUseCase {
    CheckoutSessionResponse createCheckOutSession(CreateCheckoutSessionCommand command);
}
