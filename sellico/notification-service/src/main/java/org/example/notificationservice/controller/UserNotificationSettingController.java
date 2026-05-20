package org.example.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.notificationservice.application.dto.request.UpdateNotificationSettingRequest;
import org.example.notificationservice.application.dto.response.NotificationSettingResponse;
import org.example.notificationservice.application.port.in.UserNotificationSettingUseCase;
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
@RequestMapping("/api/v1/notifications/settings")
@RequiredArgsConstructor
public class UserNotificationSettingController {

    private final UserNotificationSettingUseCase settingUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'USER')")
    public ResponseEntity<ApiResponse<List<NotificationSettingResponse>>> getSettings(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(settingUseCase.getSettings(userId)));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SELLER', 'USER')")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateSetting(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNotificationSettingRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(settingUseCase.updateSetting(userId, request)));
    }
}
