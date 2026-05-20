package org.example.identityservice.service.iml;

import feign.FeignException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.identityservice.dto.request.BuyerRegisterRequest;
import org.example.identityservice.dto.request.UserProfileCreationRequest;
import org.example.identityservice.dto.response.UserProfileResponse;
import org.example.identityservice.exception.ExternalServiceException;
import org.example.identityservice.repository.httpclient.UserClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileIntegrationService {

    private final UserClient profileClient;

    @Retry(name = "profileRetry")
    public UserProfileResponse createProfileWithRetry(String userId, BuyerRegisterRequest registerRequest) {
        log.info("Attempting to create profile for user: {} (Retry-able)", userId);

        UserProfileCreationRequest profileRequest = UserProfileCreationRequest.builder()
                .authId(UUID.fromString(userId))
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .phone(registerRequest.getPhone())
                .userType("BUYER")
                .build();

        try {
            return profileClient.createProfile(profileRequest).getData();
        } catch (ExternalServiceException e) {
            // Only treat as "already exists" if the error code indicates a duplicate user/profile,
            // NOT when it's a phone/email uniqueness violation
            if (e.getErrorCode().getStatusCode() == 409
                    && "USER_ALREADY_EXISTS".equals(e.getErrorCode().getCode())) {
                log.warn("Profile already exists for authId={}, fetching existing profile", userId);
                return profileClient.getUserByAuthId(UUID.fromString(userId)).getData();
            }
            throw e;
        } catch (FeignException.Conflict e) {
            // Fallback: handle raw FeignException 409 in case decoder fails
            // Only safe to fetch by authId if we're sure it's a duplicate user
            log.warn("Profile conflict (FeignException.Conflict) for authId={}, fetching existing profile", userId);
            return profileClient.getUserByAuthId(UUID.fromString(userId)).getData();
        }
    }

    public String getUserNameByIdentifier(String identifier) {
        log.info("Fetching username for identifier: {}", identifier);
        return profileClient.getUserName(identifier);
    }

    /**
     * Sync a Google-authenticated user's profile to user-service.
     * Uses "find or create" logic: if user already exists (by authId), it's a no-op.
     * If new, creates the user profile.
     */
    public void syncGoogleUserProfile(String keycloakUserId, String email,
                                      String username, String givenName, String familyName) {
        log.info("Syncing Google user profile: keycloakId={}, email={}", keycloakUserId, email);

        try {
            // Try to check if user profile already exists
            profileClient.getUserByAuthId(UUID.fromString(keycloakUserId));
            log.info("Google user profile already exists for keycloakId={}", keycloakUserId);
        } catch (Exception e) {
            // Profile doesn't exist yet, create it
            log.info("Creating profile for first-time Google user: keycloakId={}, email={}", keycloakUserId, email);

            // Build a meaningful username from Google data
            String effectiveUsername = username;
            if (effectiveUsername == null || effectiveUsername.isBlank()) {
                effectiveUsername = email != null ? email.split("@")[0] : "google_user_" + keycloakUserId.substring(0, 8);
            }

            UserProfileCreationRequest profileRequest = UserProfileCreationRequest.builder()
                    .authId(UUID.fromString(keycloakUserId))
                    .email(email)
                    .username(effectiveUsername)
                    .userType("BUYER")
                    .build();

            try {
                UserProfileResponse created = profileClient.createProfile(profileRequest).getData();
                log.info("Created profile for Google user: userId={}, email={}", created.getId(), email);
            } catch (Exception createEx) {
                log.error("Failed to create profile for Google user: keycloakId={}", keycloakUserId, createEx);
                throw createEx;
            }
        }
    }
}