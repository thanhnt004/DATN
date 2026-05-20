package org.example.notificationservice.infrastructure.persistence;

import org.example.notificationservice.domain.model.NotificationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<NotificationAuditLog, UUID> {

    List<NotificationAuditLog> findByNotificationIdOrderByCreatedAtDesc(UUID notificationId);
}

