package com.example.paymentservice.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ConfirmCodPaymentCommand {
    private final UUID paymentId;
    private final UUID confirmedBy; // seller or delivery agent user ID
    private final String note;
}

