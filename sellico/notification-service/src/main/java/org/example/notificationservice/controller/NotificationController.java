package org.example.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.application.NotificationService;
import org.example.notificationservice.controller.dto.NotificationResponse;
import org.example.notificationservice.controller.dto.SendNotificationRequest;
import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationStatus;
import org.example.notificationservice.domain.port.in.command.SendEmailCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Send an email notification
     */
    @PostMapping("/email")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendEmail(
            @Valid @RequestBody SendNotificationRequest request) {
        Notification notification = notificationService.sendEmail(
                SendEmailCommand.builder()
                        .userId(request.getUserId())
                        .recipientEmail(request.getRecipientEmail())
                        .notificationType(request.getNotificationType())
                        .payload(request.getPayload())
                        .referenceId(request.getReferenceId())
                        .priority(request.getPriority())
                        .language(request.getLanguage())
                        .build()
        );
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.from(notification)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(@PathVariable("id") UUID id) {
        Notification notification = notificationService.getNotification(id);
        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.from(notification)));
    }

    /**
     * Get notifications for the current authenticated user
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'USER')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId, page, size);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('SELLER', 'USER')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Get notifications for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId, page, size);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Count unread notifications for a user
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Long>> countUnreadNotifications(@PathVariable("userId") UUID userId) {
        long count = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Mark notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable("id") UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Retry a failed notification
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<Void>> retryNotification(@PathVariable("id") UUID id) {
        notificationService.retryNotification(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
