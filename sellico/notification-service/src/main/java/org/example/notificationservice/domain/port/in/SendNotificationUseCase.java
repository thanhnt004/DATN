package org.example.notificationservice.domain.port.in;

import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.port.in.command.SendEmailCommand;
import org.example.notificationservice.domain.port.in.command.SendPushCommand;

import java.util.UUID;

public interface SendNotificationUseCase {

    /**
     * Create and send an email notification
     */
    Notification sendEmail(SendEmailCommand command);

    /**
     * Create and send a push notification
     */
    Notification sendPush(SendPushCommand command);

    /**
     * Retry a failed notification
     */
    void retryNotification(UUID notificationId);

    /**
     * Process pending notifications (scheduled job)
     */
    void processPendingNotifications();

    /**
     * Send broadcast notification to multiple users
     */
    void broadcast(org.example.notificationservice.application.dto.request.BroadcastRequest request);
}

