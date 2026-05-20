package org.example.userservice.service.iml;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.dto.request.*;
import org.example.userservice.dto.response.UserProfileResponse;
import org.example.userservice.dto.response.UserPublicResponse;
import org.example.userservice.entity.*;
import org.example.userservice.exception.AuthenException;
import org.example.userservice.exception.ErrorCode;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.repository.UserProfileRepository;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.repository.UserStatusHistoryRepository;
import org.example.userservice.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileServiceIml implements UserProfileService {
    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    UserStatusHistoryRepository statusHistoryRepository;
    UserMapper userMapper;

    // =====================================================
    // Basic Profile Operations
    // =====================================================

    @Override
    public String getUserNameByIdentifier(String identifier) throws AuthenException {
        String username = userRepository.findUsernameByIdentifier(identifier);
        if (username != null) {
            return username;
        } else {
            throw new AuthenException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public UserProfileResponse createUserProfile(UserProfileCreationRequest request) {
        // Check if user profile already exists for this authId
        if (userRepository.findByAuthId(request.getAuthId()).isPresent())
            throw new AuthenException(ErrorCode.USER_ALREADY_EXISTS);

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail()))
            throw new AuthenException(ErrorCode.EMAIL_ALREADY_EXISTS);

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && userRepository.existsByUsername(request.getUsername()))
            throw new AuthenException(ErrorCode.USERNAME_ALREADY_EXISTS);

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone()))
            throw new AuthenException(ErrorCode.PHONE_ALREADY_EXISTS);

        User user = User.builder()
                .id(request.getAuthId())
                .authId(request.getAuthId())
                .userType(UserType.valueOf(request.getUserType()))
                .phone(request.getPhone())
                .username(request.getUsername())
                .email(request.getEmail())
                .build();
        userRepository.save(user);

        UserProfile userProfile = UserProfile.builder()
                .fullName(request.getUsername())
                .user(user)
                .build();
        userProfileRepository.save(userProfile);

        log.info("Created user profile for user: {}", user.getId());
        return userMapper.toResponse(user, userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return userMapper.toResponse(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicResponse getPublicProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return UserPublicResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType() != null ? user.getUserType().name() : null)
                .fullName(profile != null ? profile.getFullName() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByAuthId(UUID authId) {
        User user = userRepository.findByAuthId(authId)
                .orElseThrow(() -> new AuthenException(ErrorCode.USER_NOT_FOUND));
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
        return userMapper.toResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder().user(user).build();
                    return userProfileRepository.save(newProfile);
                });

        // Update phone if provided and different
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndIdNot(request.getPhone(), userId)) {
                throw new AuthenException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(request.getPhone());
        }

        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
        userProfileRepository.save(profile);

        log.info("Updated profile for user: {}", userId);
        return userMapper.toResponse(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getUserIdByAuthId(UUID authId) {
        return userRepository.findByAuthId(authId)
                .map(User::getId)
                .orElseThrow(() -> new AuthenException(ErrorCode.USER_NOT_FOUND));
    }

    // =====================================================
    // Change Email
    // =====================================================

    @Override
    @Transactional
    public UserProfileResponse changeEmail(UUID userId, ChangeEmailRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getNewEmail().equalsIgnoreCase(user.getEmail())) {
            throw new AuthenException(ErrorCode.SAME_EMAIL);
        }

        if (userRepository.existsByEmailAndIdNot(request.getNewEmail(), userId)) {
            throw new AuthenException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setEmail(request.getNewEmail());
        user.setVerified(false); // Require re-verification
        userRepository.save(user);

        log.info("Email changed for user: {} to {}", userId, request.getNewEmail());

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return userMapper.toResponse(user, profile);
    }

    // =====================================================
    // Deactivate Account (self-service)
    // =====================================================

    @Override
    @Transactional
    public void deactivateAccount(UUID userId, DeactivateAccountRequest request) {
        User user = findUserOrThrow(userId);

        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new AuthenException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        saveStatusHistory(user, oldStatus, UserStatus.DEACTIVATED, request.getReason(), userId);

        log.info("Account deactivated for user: {}, reason: {}", userId, request.getReason());
    }

    // =====================================================
    // Admin Operations
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> listUsers(String keyword, String status, int page, int size) {
        UserStatus userStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid status, treat as null
            }
        }

        String searchKeyword = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<User> users = userRepository.searchUsers(searchKeyword, userStatus,
                PageRequest.of(page, size));

        return users.map(user -> {
            UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);
            return userMapper.toResponse(user, profile);
        });
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserStatus(UUID userId, AdminUpdateUserStatusRequest request, UUID adminId) {
        User user = findUserOrThrow(userId);
        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            throw new AuthenException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        user.setStatus(newStatus);

        // If banning, set deletedAt; if reactivating, clear it
        if (newStatus == UserStatus.BANNED || newStatus == UserStatus.DEACTIVATED) {
            user.setDeletedAt(Instant.now());
        } else if (newStatus == UserStatus.ACTIVE) {
            user.setDeletedAt(null);
        }

        userRepository.save(user);
        saveStatusHistory(user, oldStatus, newStatus, request.getReason(), adminId);

        log.info("Admin {} changed user {} status from {} to {}", adminId, userId, oldStatus, newStatus);

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return userMapper.toResponse(user, profile);
    }

    // =====================================================
    // Internal Operations
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<UserPublicResponse> getBatchUsers(List<UUID> userIds) {
        List<User> users = userRepository.findAllByIdIn(userIds);

        // Batch load profiles
        List<UUID> foundIds = users.stream().map(User::getId).toList();
        Map<UUID, UserProfile> profileMap = userProfileRepository.findAllById(foundIds)
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));

        return users.stream()
                .map(user -> {
                    UserProfile profile = profileMap.get(user.getId());
                    return UserPublicResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .userType(user.getUserType() != null ? user.getUserType().name() : null)
                            .fullName(profile != null ? profile.getFullName() : null)
                            .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                            .createdAt(user.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional
    public void updateUserType(UUID authId, String userType) {
        User user = userRepository.findByAuthId(authId)
                .orElseThrow(() -> new AuthenException(ErrorCode.USER_NOT_FOUND));
        UserType newType = UserType.valueOf(userType);
        user.setUserType(newType);
        userRepository.save(user);
        log.info("Updated userType for authId={} to {}", authId, newType);
    }

    // =====================================================
    // Helpers
    // =====================================================

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenException(ErrorCode.USER_NOT_FOUND));
    }

    private void saveStatusHistory(User user, UserStatus oldStatus, UserStatus newStatus,
                                   String reason, UUID changedBy) {
        UserStatusHistory history = new UserStatusHistory();
        history.setId(UUID.randomUUID());
        history.setUser(user);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setChangedBy(changedBy);
        statusHistoryRepository.save(history);
    }
}
