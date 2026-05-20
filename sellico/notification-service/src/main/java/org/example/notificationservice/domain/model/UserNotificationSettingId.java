package org.example.notificationservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Cực kỳ quan trọng để Hibernate so sánh khóa
public class UserNotificationSettingId implements Serializable {

    private UUID userId;
    private String notificationType;
    private ChannelType channelType;
}