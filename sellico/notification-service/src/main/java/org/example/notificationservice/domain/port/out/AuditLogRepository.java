package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.NotificationAuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

    NotificationAuditLog save(NotificationAuditLog auditLog);

    List<NotificationAuditLog> findByNotificationId(UUID notificationId);
}

