package org.example.notificationservice.domain.port.in;

import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;

import java.util.List;
import java.util.UUID;

public interface QueryNotificationUseCase {

    /**
     * Get notification by id
     */
    Notification getNotification(UUID notificationId);

    /**
     * Get notifications by user id
     */
    List<Notification> getNotificationsByUserId(UUID userId, int page, int size);

    /**
     * Get notifications by status
     */
    List<Notification> getNotificationsByStatus(NotificationStatus status, int limit);

    /**
     * Count unread notifications for user
     */
    long countUnreadNotifications(UUID userId);

    /**
     * Mark notification as read
     */
    void markAsRead(UUID notificationId);

    /**
     * Mark all notifications as read for the given user
     */
    void markAllAsRead(UUID userId);

    /**
     * Get all notifications (for admin)
     */
    java.util.List<Notification> getAllNotifications(int page, int size);
}

