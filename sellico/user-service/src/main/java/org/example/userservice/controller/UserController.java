package org.example.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.userservice.dto.request.ChangeEmailRequest;
import org.example.userservice.dto.request.DeactivateAccountRequest;
import org.example.userservice.dto.request.UpdateProfileRequest;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserPublicResponse;
import org.example.userservice.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.UUID;

/**
 * Public API controller for user profile management
 * Base path: /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    // =====================================================
    // Current User (me) Endpoints
    // =====================================================

    /**
     * GET /api/v1/users/me - Get current user's full profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        UUID authId = UUID.fromString(jwt.getSubject());
        UserProfileResponse response = userProfileService.getProfileByAuthId(authId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/v1/users/me - Update current user's profile
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getUserId(jwt);
        UserProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PATCH /api/v1/users/me/email - Change current user's email
     */
    @PatchMapping("/me/email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> changeEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeEmailRequest request) {
        UUID userId = getUserId(jwt);
        UserProfileResponse response = userProfileService.changeEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/users/me/deactivate - Deactivate current user's account
     */
    @PostMapping("/me/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeactivateAccountRequest request) {
        UUID userId = getUserId(jwt);
        userProfileService.deactivateAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =====================================================
    // Public Profile Endpoints
    // =====================================================

    /**
     * GET /api/v1/users/{userId} - Get public user profile (limited info)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getPublicProfile(
            @PathVariable("userId") UUID userId) {
        UserPublicResponse response = userProfileService.getPublicProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Helper
    // =====================================================

    private UUID getUserId(Jwt jwt) {
        UUID authId = UUID.fromString(jwt.getSubject());
        return userProfileService.getUserIdByAuthId(authId);
    }
}
