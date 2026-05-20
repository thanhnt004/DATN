package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.ChannelStatus;
import org.example.notificationservice.domain.model.NotificationChannelLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelLogRepository {

    NotificationChannelLog save(NotificationChannelLog log);

    Optional<NotificationChannelLog> findById(UUID id);

    List<NotificationChannelLog> findByNotificationId(UUID notificationId);

    List<NotificationChannelLog> findByStatusAndRetryCountLessThan(ChannelStatus status, int maxRetryCount, int limit);

    void updateStatus(UUID logId, ChannelStatus status, String errorMessage);

    void incrementRetryCount(UUID logId);
}

