package org.example.notificationservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.notificationservice.domain.model.ChannelType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationSettingRequest {
    private String notificationType;
    private ChannelType channelType;
    private boolean enabled;
}
