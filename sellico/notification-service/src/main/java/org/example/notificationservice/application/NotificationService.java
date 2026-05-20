package org.example.notificationservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.domain.exception.NotificationException;
import org.example.notificationservice.domain.model.*;
import org.example.notificationservice.domain.port.in.QueryNotificationUseCase;
import org.example.notificationservice.domain.port.in.SendNotificationUseCase;
import org.example.notificationservice.domain.port.in.command.SendEmailCommand;
import org.example.notificationservice.domain.port.in.command.SendPushCommand;
import org.example.notificationservice.domain.port.out.*;
import org.example.notificationservice.domain.service.TemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements SendNotificationUseCase, QueryNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;
    private final ChannelLogRepository channelLogRepository;
    private final ProviderRepository providerRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmailSenderPort emailSenderPort;
    private final PushSenderPort pushSenderPort;
    private final TemplateRenderer templateRenderer;
    private final UserNotificationSettingRepository settingRepository;

    // ==================== SendNotificationUseCase ====================

    @Override
    @Transactional
    public Notification sendEmail(SendEmailCommand command) {
        log.info("Sending email notification: type={}, userId={}",
                command.getNotificationType(), command.getUserId());

        // 0. Check user settings
        if (command.getUserId() != null) {
            boolean isEnabled = settingRepository.findByUserIdAndNotificationTypeAndChannelType(
                            command.getUserId(), command.getNotificationType(), ChannelType.EMAIL)
                    .map(UserNotificationSetting::isEnabled)
                    .orElse(true); // Default to enabled if no setting found

            if (!isEnabled) {
                log.info("Notification disabled by user settings: type={}, userId={}",
                        command.getNotificationType(), command.getUserId());
                return Notification.builder()
                        .userId(command.getUserId())
                        .notificationType(command.getNotificationType())
                        .status(NotificationStatus.CANCELLED)
                        .createdAt(Instant.now())
                        .build();
            }
        }

        // 1. Find template
        NotificationTemplate template = templateRepository
                .findByNotificationTypeAndChannelTypeAndLanguage(
                        command.getNotificationType(),
                        ChannelType.EMAIL,
                        command.getLanguage())
                .orElseThrow(NotificationException::templateNotFound);

        // 2. Render template
        String subject = templateRenderer.render(template.getSubjectTemplate(), command.getPayload());
        String body = templateRenderer.render(template.getBodyTemplate(), command.getPayload());

        // 3. Create notification
        Notification notification = Notification.builder()
                .userId(command.getUserId())
                .notificationType(command.getNotificationType())
                .templateId(template.getId())
                .payload(command.getPayload())
                .priority(command.getPriority())
                .status(NotificationStatus.PROCESSING)
                .referenceId(command.getReferenceId())
                .createdAt(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        // 4. Create audit log
        auditLogRepository.save(NotificationAuditLog.create(
                notification.getId(), "CREATED", "Email notification created"));

        // 5. Find provider
        List<Provider> providers = providerRepository
                .findByChannelTypeAndIsActiveOrderByPriority(ChannelType.EMAIL, true);
        if (providers.isEmpty()) {
            throw NotificationException.providerNotFound();
        }
        Provider provider = providers.get(0);

        // 6. Create channel log
        NotificationChannelLog channelLog = new NotificationChannelLog();
        channelLog.setNotification(notification);
        channelLog.setChannelType(ChannelType.EMAIL);
        channelLog.setProviderId(provider.getId());
        channelLog.setRecipientAddress(command.getRecipientEmail());
        channelLog.setStatus(ChannelStatus.PENDING);
        channelLog = channelLogRepository.save(channelLog);

        // 7. Send email
        try {
            EmailSenderPort.SendEmailResult result = emailSenderPort.sendEmail(
                    EmailSenderPort.SendEmailRequest.builder()
                            .to(command.getRecipientEmail())
                            .subject(subject)
                            .htmlContent(body)
                            .build()
            );

            if (result.isSuccess()) {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.SENT, null);
                notificationRepository.updateStatus(notification.getId(), NotificationStatus.SENT);
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "SENT", "Email sent successfully. MessageId: " + result.getMessageId()));
                log.info("Email sent successfully: notificationId={}", notification.getId());
            } else {
                handleSendFailure(notification, channelLog, result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            handleSendFailure(notification, channelLog, e.getMessage());
        }

        return notificationRepository.findById(notification.getId()).orElse(notification);
    }

    private void handleSendFailure(Notification notification, NotificationChannelLog channelLog, String errorMessage) {
        channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, errorMessage);
        notificationRepository.updateStatus(notification.getId(), NotificationStatus.FAILED);
        auditLogRepository.save(NotificationAuditLog.create(
                notification.getId(), "FAILED", "Email send failed: " + errorMessage));
    }

    @Override
    @Transactional
    public Notification sendPush(SendPushCommand command) {
        log.info("Sending push notification: type={}, userId={}",
                command.getNotificationType(), command.getUserId());

        // 0. Check user settings
        if (command.getUserId() != null) {
            boolean isEnabled = settingRepository.findByUserIdAndNotificationTypeAndChannelType(
                            command.getUserId(), command.getNotificationType(), ChannelType.PUSH)
                    .map(UserNotificationSetting::isEnabled)
                    .orElse(true);

            if (!isEnabled) {
                log.info("Push notification disabled by user settings: type={}, userId={}",
                        command.getNotificationType(), command.getUserId());
                return Notification.builder()
                        .userId(command.getUserId())
                        .notificationType(command.getNotificationType())
                        .status(NotificationStatus.CANCELLED)
                        .createdAt(Instant.now())
                        .build();
            }
        }

        // 1. Find template (Optional for PUSH if title/body provided)
        String title = command.getTitle();
        String body = command.getBody();
        UUID templateId = null;

        if (title == null || body == null) {
            NotificationTemplate template = templateRepository
                    .findByNotificationTypeAndChannelTypeAndLanguage(
                            command.getNotificationType(),
                            ChannelType.PUSH,
                            command.getLanguage())
                    .orElseThrow(NotificationException::templateNotFound);
            
            templateId = template.getId();
            if (title == null) title = templateRenderer.render(template.getSubjectTemplate(), command.getPayload());
            if (body == null) body = templateRenderer.render(template.getBodyTemplate(), command.getPayload());
        }

        // 2. Create notification
        Notification notification = Notification.builder()
                .userId(command.getUserId())
                .notificationType(command.getNotificationType())
                .templateId(templateId)
                .payload(command.getPayload())
                .priority(command.getPriority())
                .status(NotificationStatus.PROCESSING)
                .referenceId(command.getReferenceId())
                .createdAt(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        // 3. Create audit log
        auditLogRepository.save(NotificationAuditLog.create(
                notification.getId(), "CREATED", "Push notification created"));

        // 4. Find provider
        List<Provider> providers = providerRepository
                .findByChannelTypeAndIsActiveOrderByPriority(ChannelType.PUSH, true);
        Provider provider = providers.isEmpty() ? null : providers.get(0);

        // 5. Create channel log
        NotificationChannelLog channelLog = new NotificationChannelLog();
        channelLog.setNotification(notification);
        channelLog.setChannelType(ChannelType.PUSH);
        if (provider != null) channelLog.setProviderId(provider.getId());
        channelLog.setRecipientAddress(command.getUserId().toString());
        channelLog.setStatus(ChannelStatus.PENDING);
        channelLog = channelLogRepository.save(channelLog);

        // 6. Send push
        try {
            PushSenderPort.SendPushResult result = pushSenderPort.sendPush(
                    PushSenderPort.SendPushRequest.builder()
                            .userId(command.getUserId().toString())
                            .title(title)
                            .body(body)
                            .payload(command.getPayload())
                            .type(command.getNotificationType())
                            .build()
            );

            if (result.isSuccess()) {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.SENT, null);
                notificationRepository.updateStatus(notification.getId(), NotificationStatus.SENT);
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "SENT", "Push sent successfully. MessageId: " + result.getMessageId()));
                log.info("Push sent successfully: notificationId={}", notification.getId());
            } else {
                handleSendPushFailure(notification, channelLog, result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send push: {}", e.getMessage(), e);
            handleSendPushFailure(notification, channelLog, e.getMessage());
        }

        return notificationRepository.findById(notification.getId()).orElse(notification);
    }

    private void handleSendPushFailure(Notification notification, NotificationChannelLog channelLog, String errorMessage) {
        channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, errorMessage);
        notificationRepository.updateStatus(notification.getId(), NotificationStatus.FAILED);
        auditLogRepository.save(NotificationAuditLog.create(
                notification.getId(), "FAILED", "Push send failed: " + errorMessage));
    }

    @Override
    @Transactional
    public void retryNotification(UUID notificationId) {
        log.info("Retrying notification: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationException::notFound);

        List<NotificationChannelLog> logs = channelLogRepository.findByNotificationId(notificationId);
        for (NotificationChannelLog log : logs) {
            if (log.getStatus() == ChannelStatus.FAILED && log.getRetryCount() < log.getMaxRetries()) {
                channelLogRepository.incrementRetryCount(log.getId());
                channelLogRepository.updateStatus(log.getId(), ChannelStatus.RETRYING, null);

                // Re-send based on channel type
                if (log.getChannelType() == ChannelType.EMAIL) {
                    retrySendEmail(notification, log);
                } else if (log.getChannelType() == ChannelType.PUSH) {
                    retrySendPush(notification, log);
                }
            }
        }
    }

    private void retrySendEmail(Notification notification, NotificationChannelLog channelLog) {
        NotificationTemplate template = templateRepository.findById(notification.getTemplateId())
                .orElseThrow(NotificationException::templateNotFound);

        String subject = templateRenderer.render(template.getSubjectTemplate(), notification.getPayload());
        String body = templateRenderer.render(template.getBodyTemplate(), notification.getPayload());

        try {
            EmailSenderPort.SendEmailResult result = emailSenderPort.sendEmail(
                    EmailSenderPort.SendEmailRequest.builder()
                            .to(channelLog.getRecipientAddress())
                            .subject(subject)
                            .htmlContent(body)
                            .build()
            );

            if (result.isSuccess()) {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.SENT, null);
                notificationRepository.updateStatus(notification.getId(), NotificationStatus.SENT);
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "RETRY_SUCCESS", "Retry successful"));
            } else {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, result.getErrorMessage());
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "RETRY_FAILED", result.getErrorMessage()));
            }
        } catch (Exception e) {
            channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, e.getMessage());
        }
    }

    private void retrySendPush(Notification notification, NotificationChannelLog channelLog) {
        String title = null;
        String body = null;

        if (notification.getTemplateId() != null) {
            NotificationTemplate template = templateRepository.findById(notification.getTemplateId())
                    .orElseThrow(NotificationException::templateNotFound);
            title = templateRenderer.render(template.getSubjectTemplate(), notification.getPayload());
            body = templateRenderer.render(template.getBodyTemplate(), notification.getPayload());
        } else {
            title = "Notification";
            body = notification.getNotificationType();
        }

        try {
            PushSenderPort.SendPushResult result = pushSenderPort.sendPush(
                    PushSenderPort.SendPushRequest.builder()
                            .userId(notification.getUserId().toString())
                            .title(title)
                            .body(body)
                            .payload(notification.getPayload())
                            .type(notification.getNotificationType())
                            .build()
            );

            if (result.isSuccess()) {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.SENT, null);
                notificationRepository.updateStatus(notification.getId(), NotificationStatus.SENT);
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "RETRY_SUCCESS", "Retry push successful"));
            } else {
                channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, result.getErrorMessage());
                auditLogRepository.save(NotificationAuditLog.create(
                        notification.getId(), "RETRY_FAILED", result.getErrorMessage()));
            }
        } catch (Exception e) {
            channelLogRepository.updateStatus(channelLog.getId(), ChannelStatus.FAILED, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processPendingNotifications() {
        List<NotificationChannelLog> failedLogs = channelLogRepository
                .findByStatusAndRetryCountLessThan(ChannelStatus.FAILED, 3, 100);

        for (NotificationChannelLog channelLog : failedLogs) {
            try {
                retryNotification(channelLog.getNotification().getId());
            } catch (Exception e) {
                log.error("Failed to process pending notification: {}", e.getMessage());
            }
        }
    }

    // ==================== QueryNotificationUseCase ====================

    @Override
    @Transactional(readOnly = true)
    public Notification getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(NotificationException::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUserId(UUID userId, int page, int size) {
        return notificationRepository.findByUserId(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(int page, int size) {
        return notificationRepository.findAll(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByStatus(NotificationStatus status, int limit) {
        return notificationRepository.findByStatus(status, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(UUID userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.updateStatus(notificationId, NotificationStatus.READ);
        auditLogRepository.save(NotificationAuditLog.create(
                notificationId, "STATUS_CHANGED", "Marked as read"));
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.updateStatusByUserId(userId, NotificationStatus.READ);
    }

    @Override
    public void broadcast(org.example.notificationservice.application.dto.request.BroadcastRequest request) {
        log.info("Broadcasting notification: type={}", request.getNotificationType());
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            log.warn("Broadcast called without userIds. Sending to all users not yet implemented.");
            return;
        }

        for (UUID userId : request.getUserIds()) {
            try {
                sendEmail(org.example.notificationservice.domain.port.in.command.SendEmailCommand.builder()
                        .userId(userId)
                        .notificationType(request.getNotificationType())
                        .payload(request.getPayload())
                        .priority(request.getPriority())
                        .language(request.getLanguage())
                        .build());
            } catch (Exception e) {
                log.error("Failed to send broadcast notification to user {}: {}", userId, e.getMessage());
            }
        }
    }
}
