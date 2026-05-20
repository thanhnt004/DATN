package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.NotificationTemplate;

import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository {

    Optional<NotificationTemplate> findById(UUID id);

    Optional<NotificationTemplate> findByNotificationTypeAndChannelTypeAndLanguage(
            String notificationType, ChannelType channelType, String language);

    NotificationTemplate save(NotificationTemplate template);

    java.util.List<NotificationTemplate> findAll();

    void deleteById(UUID id);
}

