package org.example.notificationservice.domain.port.in.command;

import lombok.Builder;
import lombok.Getter;
import org.example.notificationservice.domain.model.Priority;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class SendEmailCommand {
    private UUID userId;
    private String recipientEmail;
    private String notificationType;
    private Map<String, Object> payload;
    private String referenceId;
    @Builder.Default
    private Priority priority = Priority.NORMAL;
    @Builder.Default
    private String language = "vi";
}

