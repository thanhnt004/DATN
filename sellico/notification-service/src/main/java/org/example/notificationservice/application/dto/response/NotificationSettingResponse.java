package org.example.notificationservice.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.notificationservice.domain.model.ChannelType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingResponse {
    private String notificationType;
    private ChannelType channelType;
    private boolean enabled;
    private Instant updatedAt;
}
