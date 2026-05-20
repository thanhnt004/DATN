package org.example.notificationservice.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;
import org.example.notificationservice.domain.port.out.NotificationRepository;
import org.example.notificationservice.infrastructure.persistence.JpaNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Notification> findByUserId(UUID userId, int page, int size) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Override
    public List<Notification> findByStatus(NotificationStatus status, int limit) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(0, limit));
    }

    @Override
    public long countByUserIdAndStatus(UUID userId, NotificationStatus status) {
        return jpaRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    public void updateStatus(UUID notificationId, NotificationStatus status) {
        jpaRepository.updateStatus(notificationId, status);
    }

    @Override
    public void updateStatusByUserId(UUID userId, NotificationStatus status) {
        jpaRepository.updateStatusByUserId(userId, status);
    }

    @Override
    public java.util.List<Notification> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending())).getContent();
    }
}

