package org.example.notificationservice.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.notificationservice.domain.model.Priority;

import java.util.Map;
import java.util.UUID;

@Data
public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotBlank(message = "Notification type is required")
    private String notificationType;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    private String referenceId;

    private Priority priority = Priority.NORMAL;

    private String language = "vi";
}

