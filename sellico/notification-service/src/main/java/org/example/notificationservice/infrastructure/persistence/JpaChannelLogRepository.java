package org.example.notificationservice.infrastructure.persistence;

import org.example.notificationservice.domain.model.ChannelStatus;
import org.example.notificationservice.domain.model.NotificationChannelLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaChannelLogRepository extends JpaRepository<NotificationChannelLog, UUID> {

    List<NotificationChannelLog> findByNotificationId(UUID notificationId);

    List<NotificationChannelLog> findByStatusAndRetryCountLessThan(
            ChannelStatus status, int maxRetryCount, Pageable pageable);

    @Modifying
    @Query("UPDATE NotificationChannelLog c SET c.status = :status, c.errorMessage = :errorMessage, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ChannelStatus status, @Param("errorMessage") String errorMessage);

    @Modifying
    @Query("UPDATE NotificationChannelLog c SET c.retryCount = c.retryCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementRetryCount(@Param("id") UUID id);
}

