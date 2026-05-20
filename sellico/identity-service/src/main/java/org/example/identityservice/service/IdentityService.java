package org.example.identityservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.identityservice.dto.request.BuyerRegisterRequest;
import org.example.identityservice.dto.request.ChangePasswordRequest;
import org.example.identityservice.dto.request.ForgotPasswordRequest;
import org.example.identityservice.dto.request.LoginRequest;
import org.example.identityservice.dto.request.ResendVerificationRequest;
import org.example.identityservice.dto.request.ResetPasswordRequest;
import org.example.identityservice.dto.response.BuyerRegisterResponse;
import org.example.identityservice.dto.response.TokenResponse;

public interface IdentityService {
    BuyerRegisterResponse registerBuyer(BuyerRegisterRequest registerRequest);

    TokenResponse login(LoginRequest loginRequest, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void logoutAllDevices(HttpServletRequest request, HttpServletResponse response);

    TokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response);

    /**
     * Request password reset - sends email with reset link
     * @param request contains email
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password using token
     * @param request contains token and new password
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Validate reset token
     * @param token the reset token
     * @return true if token is valid
     */
    boolean validateResetToken(String token);

    /**
     * Change password for authenticated user.
     * Verifies current password, then updates to new password in Keycloak.
     * @param keycloakUserId the Keycloak user ID (from JWT subject)
     * @param request contains currentPassword, newPassword, confirmPassword
     */
    void changePassword(String keycloakUserId, ChangePasswordRequest request);

    /**
     * Resend verification email to user via Keycloak.
     * Looks up user by email, checks if already verified, then triggers Keycloak's sendVerifyEmail.
     * @param request contains the user's email
     */
    void resendVerificationEmail(ResendVerificationRequest request);

    // =====================================================
    // Google OAuth2 Login
    // =====================================================

    /**
     * Build the Keycloak Google broker login URL.
     * @param redirectUri optional custom redirect URI after login
     * @return the full URL to redirect user to
     */
    String buildGoogleLoginUrl(String redirectUri);

    /**
     * Handle the Google OAuth callback (server-side redirect flow).
     * Exchanges the authorization code for tokens via Keycloak,
     * syncs user profile if first login, and returns tokens.
     * @param code the authorization code from Keycloak
     * @return TokenResponse with access & refresh tokens
     */
    TokenResponse handleGoogleCallback(String code);

    /**
     * Handle Google OAuth callback with a custom redirect URI (SPA flow).
     * @param code the authorization code
     * @param redirectUri the redirect_uri used in the original auth request
     * @return TokenResponse with access & refresh tokens
     */
    TokenResponse handleGoogleCallback(String code, String redirectUri);

    /**
     * Get frontend URL for successful Google login redirect.
     */
    String getGoogleSuccessRedirectUrl();

    /**
     * Get frontend URL for failed Google login redirect.
     */
    String getGoogleErrorRedirectUrl();

    // =====================================================
    // Facebook OAuth2 Login
    // =====================================================

    /** Build the Keycloak Facebook broker login URL. */
    String buildFacebookLoginUrl(String redirectUri);

    /** Handle Facebook OAuth callback (server-side redirect flow). */
    TokenResponse handleFacebookCallback(String code);

    /** Handle Facebook OAuth callback with explicit redirect URI (SPA flow). */
    TokenResponse handleFacebookCallback(String code, String redirectUri);

    String getFacebookSuccessRedirectUrl();

    String getFacebookErrorRedirectUrl();
}
