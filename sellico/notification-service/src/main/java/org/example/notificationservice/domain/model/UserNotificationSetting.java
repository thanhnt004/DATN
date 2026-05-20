package org.example.notificationservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notification_settings")
@IdClass(UserNotificationSettingId.class)
@Getter
@Setter
public class UserNotificationSetting {
    @Id
    private UUID userId;
    @Id private String notificationType;
    @Id @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) private ChannelType channelType;

    private boolean isEnabled = true;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
