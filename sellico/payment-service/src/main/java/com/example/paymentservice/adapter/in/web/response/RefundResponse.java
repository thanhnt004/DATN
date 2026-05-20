package com.example.paymentservice.adapter.in.web.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefundResponse {
    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private Instant createdAt;
}

