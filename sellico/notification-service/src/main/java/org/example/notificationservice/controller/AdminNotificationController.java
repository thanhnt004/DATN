package org.example.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.application.NotificationService;
import org.example.notificationservice.application.TemplateService;
import org.example.notificationservice.application.dto.request.BroadcastRequest;
import org.example.notificationservice.controller.dto.NotificationResponse;
import org.example.notificationservice.domain.model.Notification;
import org.example.notificationservice.domain.model.NotificationTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final TemplateService templateService;

    // ==================== Template Management ====================

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationTemplate>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllTemplates()));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplate>> createTemplate(
            @Valid @RequestBody NotificationTemplate template) {
        return ResponseEntity.ok(ApiResponse.success(templateService.createTemplate(template)));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplate>> updateTemplate(
            @PathVariable("id") UUID id,
            @RequestBody NotificationTemplate template) {
        return ResponseEntity.ok(ApiResponse.success(templateService.updateTemplate(id, template)));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable("id") UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== Notification History ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<Notification> notifications = notificationService.getAllNotifications(page, size);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ==================== Broadcast ====================

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> broadcast(@Valid @RequestBody BroadcastRequest request) {
        notificationService.broadcast(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
