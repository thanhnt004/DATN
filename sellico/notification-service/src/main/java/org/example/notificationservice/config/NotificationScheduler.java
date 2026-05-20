package org.example.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.domain.port.in.SendNotificationUseCase;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final SendNotificationUseCase sendNotificationUseCase;

    /**
     * Retry failed notifications every 5 minutes
     */
    @Scheduled(fixedDelayString = "${notification.retry.interval:300000}")
    public void processFailedNotifications() {
        log.info("Starting scheduled retry of failed notifications...");
        try {
            sendNotificationUseCase.processPendingNotifications();
        } catch (Exception e) {
            log.error("Error processing failed notifications: {}", e.getMessage(), e);
        }
        log.info("Finished scheduled retry of failed notifications");
    }
}

