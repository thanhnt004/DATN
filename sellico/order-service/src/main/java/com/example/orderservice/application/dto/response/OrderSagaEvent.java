package com.example.orderservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event object for order saga progress updates
 * Used for SSE streaming to frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSagaEvent {
    
    private UUID orderId;
    private String orderNumber;
    private SagaStatus status;
    private String currentStep;
    private String message;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    public enum SagaStatus {
        PROCESSING,      // Saga is in progress
        COMPLETED,       // Saga completed successfully
        FAILED,          // Saga failed
        COMPENSATING,    // Compensation in progress
        COMPENSATED      // Compensation completed
    }
    
    public static OrderSagaEvent processing(UUID orderId, String orderNumber, String step) {
        return OrderSagaEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .status(SagaStatus.PROCESSING)
                .currentStep(step)
                .message("Processing: " + step)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static OrderSagaEvent completed(UUID orderId, String orderNumber) {
        return OrderSagaEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .status(SagaStatus.COMPLETED)
                .currentStep("COMPLETED")
                .message("Order created successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static OrderSagaEvent failed(UUID orderId, String orderNumber, String error) {
        return OrderSagaEvent.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .status(SagaStatus.FAILED)
                .currentStep("FAILED")
                .message("Order creation failed")
                .errorMessage(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
