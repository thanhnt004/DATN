package org.example.notificationservice.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.NotificationTemplate;
import org.example.notificationservice.domain.port.out.TemplateRepository;
import org.example.notificationservice.infrastructure.persistence.JpaTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TemplateRepositoryAdapter implements TemplateRepository {

    private final JpaTemplateRepository jpaRepository;

    @Override
    public Optional<NotificationTemplate> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<NotificationTemplate> findByNotificationTypeAndChannelTypeAndLanguage(
            String notificationType, ChannelType channelType, String language) {
        return jpaRepository.findByNotificationTypeAndChannelTypeAndLanguageAndIsActiveTrue(
                notificationType, channelType, language);
    }

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public java.util.List<NotificationTemplate> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

