package org.example.identityservice.service.iml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.identityservice.dto.request.BuyerRegisterRequest;
import org.example.identityservice.dto.request.ChangePasswordRequest;
import org.example.identityservice.dto.request.ForgotPasswordRequest;
import org.example.identityservice.dto.request.LoginRequest;
import org.example.identityservice.dto.request.ResendVerificationRequest;
import org.example.identityservice.dto.request.ResetPasswordRequest;
import org.example.identityservice.dto.request.UserProfileCreationRequest;
import org.example.identityservice.dto.response.BuyerRegisterResponse;
import org.example.identityservice.dto.response.TokenResponse;
import org.example.identityservice.dto.response.UserProfileResponse;
import org.example.identityservice.entity.PasswordResetToken;
import org.example.identityservice.event.model.PasswordResetRequestDTO;
import org.example.identityservice.exception.AuthErrorCode;
import org.example.identityservice.exception.ExternalServiceException;
import org.example.identityservice.exception.KeycloakErrorNormalizer;
import org.example.identityservice.exception.KeycloakException;
import org.example.identityservice.repository.PasswordResetTokenRepository;
import org.example.identityservice.repository.httpclient.KeycloakTokenClient;
import org.example.identityservice.repository.httpclient.UserClient;
import org.example.identityservice.service.IdentityService;
import org.example.identityservice.utils.CookieUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import response.CommonErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityServiceIml implements IdentityService {
    private final Keycloak keycloak;
    private final KeycloakErrorNormalizer errorNormalizer;
    private final KeycloakTokenClient keycloakTokenClient;
    private final CookieUtils cookieUtils;
    private final ProfileIntegrationService profileIntegrationService;
    private final org.example.identityservice.event.producer.IdentityProducer identityProducer;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.keycloak.server-url}") String serverUrl;
    @Value("${app.keycloak.public-url:${app.keycloak.server-url}}") String publicUrl;
    @Value("${app.keycloak.client-id}") String clientId;
    @Value("${app.keycloak.client-secret}") String clientSecret;
    @Value("${app.keycloak.realm}")
    private String realm;


    @Override
    public BuyerRegisterResponse registerBuyer(BuyerRegisterRequest registerRequest) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setEnabled(true);
        user.setRequiredActions(Arrays.asList("VERIFY_EMAIL"));

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() != 201) {
                // Đọc response lỗi
                String errorBody = response.readEntity(String.class);
                log.error("Keycloak Error: Status={}, Body={}", response.getStatus(), errorBody);

                try {
                    // Sử dụng ObjectMapper (Jackson) để parse thủ công từ chuỗi String trên
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(errorBody);

                    // Keycloak thường trả về { "errorMessage": "..." } hoặc { "error": "..." }
                    String message = node.has("errorMessage")
                            ? node.get("errorMessage").asText()
                            : (node.has("error") ? node.get("error").asText() : "Unknown error");

                    throw errorNormalizer.normalize(message);
                } catch (JsonProcessingException e) {
                    // Nếu không parse được JSON (ví dụ Keycloak trả về HTML hoặc plain text)
                    log.warn("Could not parse Keycloak error body: {}", e.getMessage());
                    throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
                }
            }

            // --- Xử lý khi thành công ---
            String userId = CreatedResponseUtil.getCreatedId(response);

            // Reset password trước (nhanh, cần thiết)
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(registerRequest.getPassword());
            keycloak.realm(realm).users().get(userId).resetPassword(passwordCred);

            try {
                UserProfileResponse userProfile = profileIntegrationService.createProfileWithRetry(userId, registerRequest);

                // Gửi email xác thực bất đồng bộ — không block response
                sendVerifyEmailAsync(userId);

                // Publish event khi đăng ký thành công
                org.example.identityservice.event.model.UserRegistrationDTO registrationEvent = new org.example.identityservice.event.model.UserRegistrationDTO();
                registrationEvent.setUserId(userProfile.getId());
                registrationEvent.setEmail(userProfile.getEmail());
                registrationEvent.setVerificationToken(userId);

                identityProducer.publishUserCreatedEvent(registrationEvent);
                log.info("Published USER_CREATED event for user: {}", userProfile.getId());

                return BuyerRegisterResponse.builder()
                        .userId(userProfile.getId())
                        .email(userProfile.getEmail())
                        .username(userProfile.getUsername())
                        .build();
            } catch (Exception e) {
                // Rollback: xóa user trong Keycloak nếu tạo profile thất bại
                try {
                    keycloak.realm(realm).users().get(userId).remove();
                } catch (Exception ex) {
                    log.error("Could not delete user from Keycloak during rollback", ex);
                }
                throw e;
            }
        }
    }

    private void sendVerifyEmailAsync(String userId) {
        final int maxRetries = 3;
        final long delayMs = 1000;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                keycloak.realm(realm).users().get(userId).sendVerifyEmail();
                log.info("Verification email sent for user: {} (attempt {})", userId, attempt);
                return;
            } catch (Exception e) {
                log.warn("Failed to send verification email for user: {} (attempt {}/{}): {}",
                        userId, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("All {} attempts to send verification email failed for user: {}", maxRetries, userId, e);
                }
            }
        }
    }


    @Override
    public TokenResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        String username = null;
        try {
            username = profileIntegrationService.getUserNameByIdentifier(loginRequest.getIdentifier());

            // Log thông số cấu hình (Không log password)
            log.info("Attempting login for user: {} (Mapped username: {}) to Realm: {} at Server: {}",
                    loginRequest.getIdentifier(), username, realm, serverUrl);
            log.debug("Using Client ID: {}, Grant Type: {}", clientId, OAuth2Constants.PASSWORD);

            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(username)
                    .password(loginRequest.getPassword())
                    .build();

            // Thực hiện lấy token
            var tokenHolder = userKeycloak.tokenManager().getAccessToken();

            log.info("Login successful for user: {}", username);

            response.addCookie(cookieUtils.createCookie(tokenHolder.getRefreshToken(), CookieUtils.REFRESH_TOKEN_COOKIE));

            return TokenResponse.builder()
                    .accessToken(tokenHolder.getToken())
                    .refreshToken(tokenHolder.getRefreshToken())
                    .expiresIn((int) tokenHolder.getExpiresIn())
                    .build();

        } catch (NotAuthorizedException e) {
            log.warn("Invalid credentials (401) for user: {}", loginRequest.getIdentifier());
            throw new KeycloakException(AuthErrorCode.INVALID_CREDENTIALS);
        } catch (jakarta.ws.rs.BadRequestException e) {
            // ĐÂY LÀ NƠI XỬ LÝ LỖI 400 BẠN ĐANG GẶP
            String errorDetail = e.getResponse().readEntity(String.class);
            log.error("Keycloak returned 400 Bad Request. Detail: {}", errorDetail);
            if (errorDetail.contains("account_temporarily_disabled")) {
                throw new KeycloakException(AuthErrorCode.ACCOUNT_LOCKED);
            } else if (errorDetail.contains("Account is not fully set up")) {
                throw new KeycloakException(AuthErrorCode.EMAIL_NOT_VERIFIED);
            } else if (errorDetail.contains("invalid_grant")) {
                throw new KeycloakException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            // Thông báo lỗi cụ thể dựa trên errorDetail (ví dụ: "invalid_grant" hoặc "unauthorized_client")
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        } catch (ExternalServiceException e) {
            log.warn("External service error during login for user {}: {}", loginRequest.getIdentifier(), e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Login failed unexpectedly for user {}: ", username, e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<Cookie> refreshTokenCookie = cookieUtils.readCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE);
            if (refreshTokenCookie.isEmpty()) {
                log.error("Refresh token cookie not found");
                throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
            }
            String refreshToken = refreshTokenCookie.get().getValue();
            if (StringUtils.hasText(refreshToken))
            {
                try {
                    // 2. Chuẩn bị tham số cho Keycloak
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("client_id", clientId);
                    params.add("client_secret", clientSecret);
                    params.add("refresh_token", refreshToken);

                    // 3. Gọi Feign Client để logout trên Keycloak Server
                    keycloakTokenClient.logout(params);
                    log.info("Keycloak session invalidated successfully.");
                } catch (Exception e) {
                    log.error("Failed to logout from Keycloak: {}", e.getMessage());
                }
            }else {
                throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
            }
            //Xoa cookie refresh token
            cookieUtils.clearCookie(response, CookieUtils.REFRESH_TOKEN_COOKIE);

        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during logout: ", e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public void logoutAllDevices(HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<Cookie> refreshTokenCookie = cookieUtils.readCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE);
            if (refreshTokenCookie.isEmpty()) {
                log.error("Refresh token cookie not found");
                throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
            }
            String refreshToken = refreshTokenCookie.get().getValue();
            if (!StringUtils.hasText(refreshToken)) {
                throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
            }
            String userId = getUserIdFromToken(refreshToken);
            // Dùng bean 'keycloak' admin (có quyền cao nhất) để logout user
            keycloak.realm(realm).users().get(userId).logout();
            log.info("User {} logged out successfully", userId);
            // Xóa cookie refresh token
            cookieUtils.clearCookie(response, CookieUtils.REFRESH_TOKEN_COOKIE);
        } catch (KeycloakException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logout failed for user ! ", e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public TokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. Lấy refresh token từ Cookie
        Optional<Cookie> refreshTokenCookie = cookieUtils.readCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie.isEmpty()) {
            log.error("Refresh token cookie not found");
            throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
        }
        String refreshToken = refreshTokenCookie.get().getValue();
        if (!StringUtils.hasText(refreshToken)) {
            throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
        }
        try {
            // 2. Chuẩn bị params gọi Keycloak
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", refreshToken);

            // 3. Gọi Keycloak exchange
            Map<String, Object> responseMap = keycloakTokenClient.exchangeToken(params);

            String newAccessToken = (String) responseMap.get("access_token");
            String newRefreshToken = (String) responseMap.get("refresh_token");
            long expiresIn = ((Number) responseMap.get("expires_in")).longValue();

            String effectiveRefreshToken = refreshToken;
            if (StringUtils.hasText(newRefreshToken)) {
                effectiveRefreshToken = newRefreshToken;
                response.addCookie(cookieUtils.createCookie(newRefreshToken, CookieUtils.REFRESH_TOKEN_COOKIE));
            } else {
                log.warn("Keycloak refresh response did not include a new refresh_token, keeping existing refresh token cookie");
            }

            return TokenResponse.builder()
                    .refreshToken(effectiveRefreshToken)
                    .accessToken(newAccessToken)
                    .expiresIn(expiresIn)
                    .build();

        } catch (Exception e) {
            log.error("Refresh token failed: {}", e.getMessage());
            throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
        }
    }
    public String getUserIdFromToken(String refreshToken) {
        try {
            AccessToken token = TokenVerifier.create(refreshToken, AccessToken.class).getToken();
            return token.getSubject(); // Trả về UserId
        } catch (Exception e) {
            throw new KeycloakException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    @Value("${app.password-reset.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.password-reset.base-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    // Google OAuth2 config
    @Value("${app.google.callback-url:http://localhost:8088/api/v1/identity/auth/google/callback}")
    private String googleCallbackUrl;

    @Value("${app.google.frontend-success-url:http://localhost:5173/oauth/callback}")
    private String googleFrontendSuccessUrl;

    @Value("${app.google.frontend-error-url:http://localhost:5173/login}")
    private String googleFrontendErrorUrl;

    // Facebook OAuth2 config
    @Value("${app.facebook.callback-url:http://localhost:8088/api/v1/identity/auth/facebook/callback}")
    private String facebookCallbackUrl;

    @Value("${app.facebook.frontend-success-url:http://localhost:5173/oauth/callback}")
    private String facebookFrontendSuccessUrl;

    @Value("${app.facebook.frontend-error-url:http://localhost:5173/login}")
    private String facebookFrontendErrorUrl;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        log.info("Processing forgot password request for email: {}", email);

        try {
            // 1. Tìm user trong Keycloak theo email
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .searchByEmail(email, true);

            if (users.isEmpty()) {
                // Không tiết lộ email không tồn tại (security best practice)
                log.warn("No user found with email: {}", email);
                return; // Vẫn trả về success để tránh email enumeration
            }

            UserRepresentation keycloakUser = users.get(0);
            String keycloakUserId = keycloakUser.getId();

            // 2. Invalidate các token cũ chưa sử dụng
            passwordResetTokenRepository.invalidateExistingTokens(keycloakUserId);

            // 3. Tạo reset token mới
            String token = UUID.randomUUID().toString();
            Instant expiresAt = Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .userId(keycloakUserId)
                    .email(email)
                    .expiresAt(expiresAt)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // 4. Lấy thông tin user profile để có tên
            String userName = keycloakUser.getFirstName() != null
                    ? keycloakUser.getFirstName()
                    : keycloakUser.getUsername();

            // 5. Tạo reset link
            String resetLink = resetPasswordBaseUrl + "?token=" + token;

            // 6. Gửi event để notification service gửi email
            PasswordResetRequestDTO passwordResetEvent = PasswordResetRequestDTO.builder()
                    .userId(UUID.fromString(keycloakUserId))
                    .email(email)
                    .name(userName)
                    .resetLink(resetLink)
                    .expiresAt(expiresAt.toString())
                    .build();

            identityProducer.publishPasswordResetEvent(passwordResetEvent);

            log.info("Password reset email sent for user: {}", email);

        } catch (Exception e) {
            log.error("Error processing forgot password request: {}", e.getMessage(), e);
            // Không throw exception để tránh tiết lộ thông tin
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset request");

        // 1. Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new KeycloakException(AuthErrorCode.PASSWORDS_NOT_MATCH);
        }

        // 2. Find and validate token
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedAtIsNull(request.getToken())
                .orElseThrow(() -> new KeycloakException(AuthErrorCode.INVALID_RESET_TOKEN));

        if (resetToken.isExpired()) {
            throw new KeycloakException(AuthErrorCode.RESET_TOKEN_EXPIRED);
        }

        try {
            // 3. Update password in Keycloak
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);

            keycloak.realm(realm)
                    .users()
                    .get(resetToken.getUserId())
                    .resetPassword(credential);

            // 4. Mark token as used
            resetToken.setUsedAt(Instant.now());
            passwordResetTokenRepository.save(resetToken);

            // 5. Logout user from all sessions (security measure)
            keycloak.realm(realm)
                    .users()
                    .get(resetToken.getUserId())
                    .logout();

            log.info("Password reset successful for user: {}", resetToken.getEmail());

        } catch (Exception e) {
            log.error("Error resetting password: {}", e.getMessage(), e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public boolean validateResetToken(String token) {
        return passwordResetTokenRepository
                .findByTokenAndUsedAtIsNull(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    @Override
    public void changePassword(String keycloakUserId, ChangePasswordRequest request) {
        log.info("Processing change password request for user: {}", keycloakUserId);

        // 1. Validate new password matches confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new KeycloakException(AuthErrorCode.PASSWORDS_NOT_MATCH);
        }

        // 2. Prevent same password
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new KeycloakException(AuthErrorCode.SAME_PASSWORD);
        }

        // 3. Verify current password by attempting to log in via Keycloak
        //    Get the username from Keycloak to perform the password check
        UserRepresentation user = keycloak.realm(realm).users().get(keycloakUserId).toRepresentation();
        if (user == null) {
            throw new KeycloakException(AuthErrorCode.USER_NOT_FOUND);
        }

        try {
            // Try to authenticate with current password to verify it's correct
            Keycloak verifyKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(user.getUsername())
                    .password(request.getCurrentPassword())
                    .build();

            // This will throw if the current password is wrong
            verifyKeycloak.tokenManager().getAccessToken();

        } catch (NotAuthorizedException e) {
            log.warn("Current password verification failed for user: {}", keycloakUserId);
            throw new KeycloakException(AuthErrorCode.INCORRECT_OLD_PASSWORD);
        } catch (jakarta.ws.rs.BadRequestException e) {
            String detail = e.getResponse().readEntity(String.class);
            log.warn("Current password verification failed (400) for user: {}. Detail: {}", keycloakUserId, detail);
            throw new KeycloakException(AuthErrorCode.INCORRECT_OLD_PASSWORD);
        } catch (Exception e) {
            log.error("Unexpected error during password verification for user: {}", keycloakUserId, e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // 4. Update password in Keycloak
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getNewPassword());
            credential.setTemporary(false);

            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .resetPassword(credential);

            // 5. Logout all other sessions for security
            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .logout();

            log.info("Password changed successfully for user: {}", keycloakUserId);

        } catch (Exception e) {
            log.error("Failed to update password in Keycloak for user: {}", keycloakUserId, e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // =====================================================
    // Resend Verification Email
    // =====================================================

    @Override
    public void resendVerificationEmail(ResendVerificationRequest request) {
        log.info("Processing resend verification email request for: {}", request.getEmail());

        // 1. Search user by email in Keycloak
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByEmail(request.getEmail(), true);

        if (users == null || users.isEmpty()) {
            // Return success even if user not found (security: don't expose user existence)
            log.warn("Resend verification: user not found for email={}", request.getEmail());
            return;
        }

        UserRepresentation user = users.get(0);

        // 2. Check if email is already verified
        if (Boolean.TRUE.equals(user.isEmailVerified())) {
            throw new KeycloakException(AuthErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 3. Check if user account is enabled
        if (!Boolean.TRUE.equals(user.isEnabled())) {
            log.warn("Resend verification: user account is disabled for email={}", request.getEmail());
            throw new KeycloakException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        // 4. Send verification email via Keycloak
        try {
            keycloak.realm(realm)
                    .users()
                    .get(user.getId())
                    .sendVerifyEmail();

            log.info("Verification email resent successfully for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", user.getId(), e);
            throw new KeycloakException(AuthErrorCode.VERIFICATION_EMAIL_SEND_FAILED);
        }
    }

    // =====================================================
    // Google OAuth2 Login Implementation
    // =====================================================

    @Override
    public String buildGoogleLoginUrl(String redirectUri) {
        // The redirect_uri is the URL Keycloak will redirect to after Google login
        String callbackUrl = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : googleCallbackUrl;

        try {
            return publicUrl + "/realms/" + realm + "/protocol/openid-connect/auth"
                    + "?client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                    + "&redirect_uri=" + java.net.URLEncoder.encode(callbackUrl, "UTF-8")
                    + "&response_type=code"
                    + "&scope=openid+email+profile"
                    + "&kc_idp_hint=google";  // Skip Keycloak login page, go directly to Google
        } catch (Exception e) {
            log.error("Failed to build Google login URL", e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public TokenResponse handleGoogleCallback(String code) {
        return handleGoogleCallback(code, googleCallbackUrl);
    }

    @Override
    public TokenResponse handleGoogleCallback(String code, String redirectUri) {
        String effectiveRedirectUri = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : googleCallbackUrl;

        try {
            // 1. Exchange authorization code for tokens via Keycloak token endpoint
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("client_id", clientId);
            tokenParams.add("client_secret", clientSecret);
            tokenParams.add("code", code);
            tokenParams.add("redirect_uri", effectiveRedirectUri);

            Map<String, Object> tokenResponse = keycloakTokenClient.exchangeToken(tokenParams);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            long expiresIn = ((Number) tokenResponse.get("expires_in")).longValue();

            if (accessToken == null) {
                log.error("No access_token returned from Keycloak for Google callback");
                throw new KeycloakException(AuthErrorCode.INVALID_CREDENTIALS);
            }

            // 2. Extract user info from the access token
            AccessToken parsedToken = TokenVerifier.create(accessToken, AccessToken.class).getToken();
            String keycloakUserId = parsedToken.getSubject();
            String email = parsedToken.getEmail();
            String preferredUsername = parsedToken.getPreferredUsername();
            String givenName = parsedToken.getGivenName();
            String familyName = parsedToken.getFamilyName();

            log.info("Google login successful: userId={}, email={}, username={}",
                    keycloakUserId, email, preferredUsername);

            // 3. Sync user profile to user-service if this is a first-time login
            syncGoogleUserProfile(keycloakUserId, email, preferredUsername, givenName, familyName);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .build();

        } catch (KeycloakException e) {
            throw e;
        } catch (ExternalServiceException e) {
            // Propagate errors from user-service (e.g. phone number already exists)
            throw e;
        } catch (Exception e) {
            log.error("Failed to handle Google OAuth callback: {}", e.getMessage(), e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public String getGoogleSuccessRedirectUrl() {
        return googleFrontendSuccessUrl;
    }

    @Override
    public String getGoogleErrorRedirectUrl() {
        return googleFrontendErrorUrl;
    }

    /**
     * Sync user profile to user-service.
     * If the user already has a profile, this is a no-op.
     * If the user is new (first Google login), creates a profile in user-service.
     * On failure, rolls back the Keycloak user and re-throws the exception.
     */
    private void syncGoogleUserProfile(String keycloakUserId, String email,
                                       String username, String givenName, String familyName) {
        try {
            profileIntegrationService.syncGoogleUserProfile(
                    keycloakUserId, email, username, givenName, familyName);
        } catch (ExternalServiceException e) {
            log.error("Failed to sync Google user profile for userId={}: {}",
                    keycloakUserId, e.getErrorCode().getMessage());
            // Rollback: delete the Keycloak user since profile creation failed
            rollbackKeycloakUser(keycloakUserId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync Google user profile for userId={}: {}",
                    keycloakUserId, e.getMessage());
            // Rollback: delete the Keycloak user since profile creation failed
            rollbackKeycloakUser(keycloakUserId);
            throw e;
        }
    }

    /**
     * Delete a Keycloak user as part of rollback when profile creation fails.
     */
    private void rollbackKeycloakUser(String keycloakUserId) {
        try {
            keycloak.realm(realm).users().get(keycloakUserId).remove();
            log.info("Rolled back Keycloak user: {}", keycloakUserId);
        } catch (Exception ex) {
            log.error("Could not delete Keycloak user during rollback for userId={}", keycloakUserId, ex);
        }
    }

    // =====================================================
    // Facebook OAuth2 Login Implementation
    // =====================================================

    @Override
    public String buildFacebookLoginUrl(String redirectUri) {
        String callbackUrl = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : facebookCallbackUrl;
        try {
            return publicUrl + "/realms/" + realm + "/protocol/openid-connect/auth"
                    + "?client_id=" + java.net.URLEncoder.encode(clientId, "UTF-8")
                    + "&redirect_uri=" + java.net.URLEncoder.encode(callbackUrl, "UTF-8")
                    + "&response_type=code"
                    + "&scope=openid+email+profile"
                    + "&kc_idp_hint=facebook"; // Skip Keycloak login page, go directly to Facebook
        } catch (Exception e) {
            log.error("Failed to build Facebook login URL", e);
            throw new KeycloakException(CommonErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public TokenResponse handleFacebookCallback(String code) {
        return handleFacebookCallback(code, facebookCallbackUrl);
    }

    @Override
    public TokenResponse handleFacebookCallback(String code, String redirectUri) {
        String effectiveRedirectUri = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : facebookCallbackUrl;
        try {
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("grant_type", "authorization_code");
            tokenParams.add("client_id", clientId);
            tokenParams.add("client_secret", clientSecret);
            tokenParams.add("code", code);
            tokenParams.add("redirect_uri", effectiveRedirectUri);

            Map<String, Object> tokenResponse = keycloakTokenClient.exchangeToken(tokenParams);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            long expiresIn = ((Number) tokenResponse.get("expires_in")).longValue();

            if (accessToken == null) {
                log.error("No access_token returned from Keycloak for Facebook callback");
                throw new KeycloakException(AuthErrorCode.FACEBOOK_AUTH_FAILED);
            }

            AccessToken parsedToken = TokenVerifier.create(accessToken, AccessToken.class).getToken();
            String keycloakUserId = parsedToken.getSubject();
            String email = parsedToken.getEmail();
            String preferredUsername = parsedToken.getPreferredUsername();
            String givenName = parsedToken.getGivenName();
            String familyName = parsedToken.getFamilyName();

            log.info("Facebook login successful: userId={}, email={}, username={}",
                    keycloakUserId, email, preferredUsername);

            syncFacebookUserProfile(keycloakUserId, email, preferredUsername, givenName, familyName);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiresIn)
                    .build();

        } catch (KeycloakException e) {
            throw e;
        } catch (ExternalServiceException e) {
            // Propagate errors from user-service (e.g. phone number already exists)
            throw e;
        } catch (Exception e) {
            log.error("Failed to handle Facebook OAuth callback: {}", e.getMessage(), e);
            throw new KeycloakException(AuthErrorCode.FACEBOOK_AUTH_FAILED);
        }
    }

    @Override
    public String getFacebookSuccessRedirectUrl() {
        return facebookFrontendSuccessUrl;
    }

    @Override
    public String getFacebookErrorRedirectUrl() {
        return facebookFrontendErrorUrl;
    }

    private void syncFacebookUserProfile(String keycloakUserId, String email,
                                         String username, String givenName, String familyName) {
        try {
            // Reuse the same profile sync logic — the user-service treats all social logins identically
            profileIntegrationService.syncGoogleUserProfile(
                    keycloakUserId, email, username, givenName, familyName);
        } catch (ExternalServiceException e) {
            log.error("Failed to sync Facebook user profile for userId={}: {}",
                    keycloakUserId, e.getErrorCode().getMessage());
            // Rollback: delete the Keycloak user since profile creation failed
            rollbackKeycloakUser(keycloakUserId);
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync Facebook user profile for userId={}: {}",
                    keycloakUserId, e.getMessage());
            // Rollback: delete the Keycloak user since profile creation failed
            rollbackKeycloakUser(keycloakUserId);
            throw e;
        }
    }
}
