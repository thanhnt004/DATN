package org.example.userservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.userservice.dto.request.UserProfileCreationRequest;
import org.example.userservice.dto.response.AddressResponse;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserPublicResponse;
import org.example.userservice.service.AddressService;
import org.example.userservice.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.util.List;
import java.util.UUID;

/**
 * Internal API controller for service-to-service communication
 * Base path: /internal/v1
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserProfileController {

    UserProfileService userProfileService;
    AddressService addressService;

    // =====================================================
    // User Profile Operations
    // =====================================================

    /**
     * POST /internal/v1/users - Create user profile (called from identity-service)
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createProfile(
            @RequestBody UserProfileCreationRequest request) {
        UserProfileResponse response = userProfileService.createUserProfile(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/user-name/{identifier} - Get username by identifier (email/phone/username)
     */
    @GetMapping("/user-name/{identifier}")
    public ResponseEntity<String> getUserName(@PathVariable("identifier") String identifier) {
        String username = userProfileService.getUserNameByIdentifier(identifier);
        return ResponseEntity.ok(username);
    }

    /**
     * GET /internal/v1/users/{userId} - Get user profile by user ID
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable("userId") UUID userId) {
        UserProfileResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/users/by-auth/{authId} - Get user profile by auth ID (Keycloak ID)
     */
    @GetMapping("/users/by-auth/{authId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileByAuthId(@PathVariable("authId") UUID authId) {
        UserProfileResponse response = userProfileService.getProfileByAuthId(authId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/users/id-by-auth/{authId} - Get user ID by auth ID
     */
    @GetMapping("/users/id-by-auth/{authId}")
    public ResponseEntity<UUID> getUserIdByAuthId(@PathVariable("authId") UUID authId) {
        UUID userId = userProfileService.getUserIdByAuthId(authId);
        return ResponseEntity.ok(userId);
    }

    /**
     * GET /internal/v1/users/{userId}/exists - Check if user exists
     */
    @GetMapping("/users/{userId}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(userProfileService.existsByUserId(userId));
    }

    // =====================================================
    // Batch Operations
    // =====================================================

    /**
     * GET /internal/v1/users/batch - Get batch user profiles by IDs
     * Used by: order-service, notification-service, etc.
     */
    @GetMapping("/users/batch")
    public ResponseEntity<ApiResponse<List<UserPublicResponse>>> getBatchUsers(
            @RequestParam("ids") List<UUID> ids) {
        List<UserPublicResponse> response = userProfileService.getBatchUsers(ids);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /internal/v1/users/batch - Get batch user profiles (POST for large lists)
     */
    @PostMapping("/users/batch")
    public ResponseEntity<ApiResponse<List<UserPublicResponse>>> getBatchUsersPost(
            @RequestBody List<UUID> ids) {
        List<UserPublicResponse> response = userProfileService.getBatchUsers(ids);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // Address Operations (for order-service)
    // =====================================================

    /**
     * GET /internal/v1/users/{userId}/addresses/default - Get default address for user
     */
    @GetMapping("/users/{userId}/addresses/default")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(@PathVariable("userId") UUID userId) {
        AddressResponse response = addressService.getDefaultAddress(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /internal/v1/users/{userId}/addresses/{addressId} - Get specific address
     */
    @GetMapping("/users/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @PathVariable("userId") UUID userId,
            @PathVariable("addressId") UUID addressId) {
        AddressResponse response = addressService.getAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =====================================================
    // User Type Operations (for seller-service)
    // =====================================================

    /**
     * PATCH /internal/v1/users/by-auth/{authId}/user-type - Update user type by auth ID
     * Called by seller-service when a seller is approved.
     */
    @PatchMapping("/users/by-auth/{authId}/user-type")
    public ResponseEntity<Void> updateUserType(
            @PathVariable("authId") UUID authId,
            @RequestBody java.util.Map<String, String> body) {
        String userType = body.get("userType");
        userProfileService.updateUserType(authId, userType);
        return ResponseEntity.ok().build();
    }
}
