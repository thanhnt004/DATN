package org.example.notificationservice.application.port.in;

import org.example.notificationservice.application.dto.request.UpdateNotificationSettingRequest;
import org.example.notificationservice.application.dto.response.NotificationSettingResponse;

import java.util.List;
import java.util.UUID;

public interface UserNotificationSettingUseCase {
    List<NotificationSettingResponse> getSettings(UUID userId);
    NotificationSettingResponse updateSetting(UUID userId, UpdateNotificationSettingRequest request);
}
