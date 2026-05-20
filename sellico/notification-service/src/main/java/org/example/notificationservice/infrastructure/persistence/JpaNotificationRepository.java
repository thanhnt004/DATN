package org.example.notificationservice.infrastructure.persistence;

import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaNotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status WHERE n.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status WHERE n.userId = :userId AND n.status <> :status")
    void updateStatusByUserId(@Param("userId") UUID userId, @Param("status") NotificationStatus status);
}

