package org.example.notificationservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.application.dto.request.UpdateNotificationSettingRequest;
import org.example.notificationservice.application.dto.response.NotificationSettingResponse;
import org.example.notificationservice.application.port.in.UserNotificationSettingUseCase;
import org.example.notificationservice.domain.model.UserNotificationSetting;
import org.example.notificationservice.domain.port.out.UserNotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNotificationSettingService implements UserNotificationSettingUseCase {

    private final UserNotificationSettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationSettingResponse> getSettings(UUID userId) {
        return settingRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationSettingResponse updateSetting(UUID userId, UpdateNotificationSettingRequest request) {
        log.info("Updating notification setting for user {}: type={}, channel={}, enabled={}",
                userId, request.getNotificationType(), request.getChannelType(), request.isEnabled());

        UserNotificationSetting setting = settingRepository.findByUserIdAndNotificationTypeAndChannelType(
                userId, request.getNotificationType(), request.getChannelType())
                .orElseGet(() -> {
                    UserNotificationSetting newSetting = new UserNotificationSetting();
                    newSetting.setUserId(userId);
                    newSetting.setNotificationType(request.getNotificationType());
                    newSetting.setChannelType(request.getChannelType());
                    return newSetting;
                });

        setting.setEnabled(request.isEnabled());
        setting.setUpdatedAt(Instant.now());

        UserNotificationSetting saved = settingRepository.save(setting);
        return mapToResponse(saved);
    }

    private NotificationSettingResponse mapToResponse(UserNotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .notificationType(setting.getNotificationType())
                .channelType(setting.getChannelType())
                .enabled(setting.isEnabled())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
