package org.example.notificationservice.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.domain.model.ChannelStatus;
import org.example.notificationservice.domain.model.NotificationChannelLog;
import org.example.notificationservice.domain.port.out.ChannelLogRepository;
import org.example.notificationservice.infrastructure.persistence.JpaChannelLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChannelLogRepositoryAdapter implements ChannelLogRepository {

    private final JpaChannelLogRepository jpaRepository;

    @Override
    public NotificationChannelLog save(NotificationChannelLog log) {
        return jpaRepository.save(log);
    }

    @Override
    public Optional<NotificationChannelLog> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<NotificationChannelLog> findByNotificationId(UUID notificationId) {
        return jpaRepository.findByNotificationId(notificationId);
    }

    @Override
    public List<NotificationChannelLog> findByStatusAndRetryCountLessThan(ChannelStatus status, int maxRetryCount, int limit) {
        return jpaRepository.findByStatusAndRetryCountLessThan(status, maxRetryCount, PageRequest.of(0, limit));
    }

    @Override
    public void updateStatus(UUID logId, ChannelStatus status, String errorMessage) {
        jpaRepository.updateStatus(logId, status, errorMessage);
    }

    @Override
    public void incrementRetryCount(UUID logId) {
        jpaRepository.incrementRetryCount(logId);
    }
}

