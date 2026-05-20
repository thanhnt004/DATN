package org.example.notificationservice.controller.dto;

import lombok.Builder;
import lombok.Data;
import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;
import org.example.notificationservice.domain.model.Priority;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String notificationType;
    private Map<String, Object> payload;
    private Priority priority;
    private NotificationStatus status;
    private String referenceId;
    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .notificationType(notification.getNotificationType())
                .payload(notification.getPayload())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

