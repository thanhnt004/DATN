package org.example.notificationservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.domain.exception.NotificationException;
import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.NotificationTemplate;
import org.example.notificationservice.domain.port.out.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(NotificationException::templateNotFound);
    }

    @Transactional(readOnly = true)
    public NotificationTemplate getTemplate(String notificationType, ChannelType channelType, String language) {
        return templateRepository.findByNotificationTypeAndChannelTypeAndLanguage(
                        notificationType, channelType, language)
                .orElseThrow(NotificationException::templateNotFound);
    }

    @Transactional(readOnly = true)
    public java.util.List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        templateRepository.deleteById(templateId);
    }

    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating template: type={}, channel={}",
                template.getNotificationType(), template.getChannelType());
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        return templateRepository.save(template);
    }

    @Transactional
    public NotificationTemplate updateTemplate(UUID templateId, NotificationTemplate updates) {
        NotificationTemplate existing = getTemplate(templateId);

        if (updates.getSubjectTemplate() != null) {
            existing.setSubjectTemplate(updates.getSubjectTemplate());
        }
        if (updates.getBodyTemplate() != null) {
            existing.setBodyTemplate(updates.getBodyTemplate());
        }
        existing.setActive(updates.isActive());
        existing.setUpdatedAt(Instant.now());

        return templateRepository.save(existing);
    }
}
