package org.example.userservice.service;

import org.example.userservice.dto.request.AdminUpdateUserStatusRequest;
import org.example.userservice.dto.request.ChangeEmailRequest;
import org.example.userservice.dto.request.DeactivateAccountRequest;
import org.example.userservice.dto.request.UpdateProfileRequest;
import org.example.userservice.dto.request.UserProfileCreationRequest;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserPublicResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserProfileService {

    /**
     * Get username by identifier (email, phone, or authId)
     */
    String getUserNameByIdentifier(String identifier);

    /**
     * Create user profile (internal use from identity-service)
     */
    UserProfileResponse createUserProfile(UserProfileCreationRequest request);

    /**
     * Get user profile by user ID (full info, for owner)
     */
    UserProfileResponse getProfile(UUID userId);

    /**
     * Get public user profile (limited info)
     */
    UserPublicResponse getPublicProfile(UUID userId);

    /**
     * Get user profile by auth ID (Keycloak ID)
     */
    UserProfileResponse getProfileByAuthId(UUID authId);

    /**
     * Update user profile
     */
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Get user ID by auth ID
     */
    UUID getUserIdByAuthId(UUID authId);

    /**
     * Change user email
     */
    UserProfileResponse changeEmail(UUID userId, ChangeEmailRequest request);

    /**
     * Deactivate user account (self-service)
     */
    void deactivateAccount(UUID userId, DeactivateAccountRequest request);

    // =====================================================
    // Admin operations
    // =====================================================

    /**
     * Admin: list all users with pagination
     */
    Page<UserProfileResponse> listUsers(String keyword, String status, int page, int size);

    /**
     * Admin: update user status (ban/unban/suspend)
     */
    UserProfileResponse updateUserStatus(UUID userId, AdminUpdateUserStatusRequest request, UUID adminId);

    // =====================================================
    // Internal operations (service-to-service)
    // =====================================================

    /**
     * Internal: get batch users by IDs
     */
    List<UserPublicResponse> getBatchUsers(List<UUID> userIds);

    /**
     * Internal: check if user exists
     */
    boolean existsByUserId(UUID userId);

    /**
     * Internal: update user type by auth ID (e.g., BUYER -> SELLER after seller approval)
     */
    void updateUserType(UUID authId, String userType);
}
