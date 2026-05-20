package org.example.notificationservice.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.domain.model.NotificationAuditLog;
import org.example.notificationservice.domain.port.out.AuditLogRepository;
import org.example.notificationservice.infrastructure.persistence.JpaAuditLogRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;

    @Override
    public NotificationAuditLog save(NotificationAuditLog auditLog) {
        return jpaRepository.save(auditLog);
    }

    @Override
    public List<NotificationAuditLog> findByNotificationId(UUID notificationId) {
        return jpaRepository.findByNotificationIdOrderByCreatedAtDesc(notificationId);
    }
}

