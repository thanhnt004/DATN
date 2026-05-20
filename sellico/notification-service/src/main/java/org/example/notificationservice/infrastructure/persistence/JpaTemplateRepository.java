package org.example.notificationservice.infrastructure.persistence;

import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByNotificationTypeAndChannelTypeAndLanguageAndIsActiveTrue(
            String notificationType, ChannelType channelType, String language);
}

