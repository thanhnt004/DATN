package org.example.notificationservice.domain.port.out;

import org.example.notificationservice.domain.model.ChannelType;
import org.example.notificationservice.domain.model.UserNotificationSetting;
import org.example.notificationservice.domain.model.UserNotificationSettingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, UserNotificationSettingId> {
    List<UserNotificationSetting> findByUserId(UUID userId);
    Optional<UserNotificationSetting> findByUserIdAndNotificationTypeAndChannelType(UUID userId, String notificationType, ChannelType channelType);
}
