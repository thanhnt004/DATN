package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    List<Notification> findByUserId(UUID userId, int page, int size);

    List<Notification> findByStatus(NotificationStatus status, int limit);

    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    void updateStatus(UUID notificationId, NotificationStatus status);

    void updateStatusByUserId(UUID userId, NotificationStatus status);

    java.util.List<Notification> findAll(int page, int size);
}

