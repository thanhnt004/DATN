package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.AdminUpdateUserStatusRequest;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

/**
 * Admin API controller for user management
 * Base path: /api/v1/admin/users
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserProfileService userProfileService;

    /**
     * GET /api/v1/admin/users - List users with search and filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<UserProfileResponse> response = userProfileService.listUsers(keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/admin/users/{userId} - Get user detail (full info)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserDetail(
            @PathVariable("userId") UUID userId
    ) {
        UserProfileResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/status - Update user status (ban/unban/suspend)
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody AdminUpdateUserStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        UserProfileResponse response = userProfileService.updateUserStatus(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

