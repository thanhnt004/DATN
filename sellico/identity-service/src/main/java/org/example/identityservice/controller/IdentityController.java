package org.example.identityservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.identityservice.dto.request.BuyerRegisterRequest;
import org.example.identityservice.dto.request.ChangePasswordRequest;
import org.example.identityservice.dto.request.ForgotPasswordRequest;
import org.example.identityservice.dto.request.LoginRequest;
import org.example.identityservice.dto.request.ResendVerificationRequest;
import org.example.identityservice.dto.request.ResetPasswordRequest;
import org.example.identityservice.dto.response.BuyerRegisterResponse;
import org.example.identityservice.dto.response.TokenResponse;
import org.example.identityservice.service.IdentityService;
import org.example.identityservice.utils.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityController {
    IdentityService identityService;
    CookieUtils cookieUtils;
    @PostMapping("auth/register/buyer")
    public ResponseEntity<ApiResponse<BuyerRegisterResponse>> registerBuyer(@RequestBody BuyerRegisterRequest request) {
        BuyerRegisterResponse response = identityService.registerBuyer(request);
        URI url = URI.create("/auth/register/buyer/" + response.getUserId());
        return ResponseEntity.created(url).body(ApiResponse.success(response));
    }
    @PostMapping("auth/login")
    public ResponseEntity<ApiResponse<TokenResponse>> loginUser(@RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = identityService.login(request,response);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }
    @PostMapping("auth/logout")
    public ResponseEntity<ApiResponse<String>> logoutUser(HttpServletResponse response, HttpServletRequest request) {
        identityService.logout(request,response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
    @PostMapping("auth/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutUserFromAllDevices(HttpServletResponse response, HttpServletRequest request) {
        identityService.logoutAllDevices(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logout from all devices successful"));
    }
    @PostMapping("auth/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(HttpServletResponse response, HttpServletRequest request) {
        TokenResponse tokenResponse = identityService.refreshToken(request,response);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    /**
     * Request password reset - sends email with reset link
     */
    @PostMapping("auth/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        identityService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a password reset link has been sent"));
    }

    /**
     * Reset password using token from email
     */
    @PostMapping("auth/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        identityService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }

    /**
     * Validate reset token (for frontend to check before showing reset form)
     */
    @GetMapping("auth/validate-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateResetToken(@RequestParam("token") String token) {
        boolean isValid = identityService.validateResetToken(token);
        return ResponseEntity.ok(ApiResponse.success(Map.of("valid", isValid)));
    }

    /**
     * PUT /identity/auth/change-password
     * Change password for authenticated user.
     * Requires: valid JWT token (Authorization header)
     */
    @PutMapping("auth/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakUserId = jwt.getSubject();
        identityService.changePassword(keycloakUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Password has been changed successfully"));
    }

    /**
     * POST /api/v1/identity/auth/resend-verification
     * Resend email verification link to user.
     * No authentication required (user may not be able to log in yet).
     */
    @PostMapping("auth/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        identityService.resendVerificationEmail(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists and is not verified, a verification email has been sent"));
    }

    // =====================================================
    // Google OAuth2 Login
    // =====================================================

    /**
     * GET /identity/auth/google
     * Redirects user to Keycloak's Google broker login page.
     * Frontend calls this endpoint to start Google OAuth flow.
     */
    @GetMapping("auth/google")
    public void redirectToGoogle(
            HttpServletResponse response,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri
    ) throws IOException {
        String googleLoginUrl = identityService.buildGoogleLoginUrl(redirectUri);
        response.sendRedirect(googleLoginUrl);
    }

    /**
     * GET /identity/auth/google/callback
     * Keycloak redirects here after Google authentication.
     * Exchanges authorization code for tokens, syncs profile, sets cookie, redirects to frontend.
     */
    @GetMapping("auth/google/callback")
    public void handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        try {
            TokenResponse tokenResponse = identityService.handleGoogleCallback(code);

            // Set refresh token in Http-Only cookie
            Cookie cookie = cookieUtils.createCookie(
                    tokenResponse.getRefreshToken(), CookieUtils.REFRESH_TOKEN_COOKIE);
            response.addCookie(cookie);

            // Redirect to frontend dashboard
            String frontendUrl = identityService.getGoogleSuccessRedirectUrl();
            response.sendRedirect(frontendUrl
                    + "?access_token=" + tokenResponse.getAccessToken()
                    + "&expires_in=" + tokenResponse.getExpiresIn());

        } catch (Exception e) {
            String errorUrl = identityService.getGoogleErrorRedirectUrl();
            response.sendRedirect(errorUrl + "?error=google_login_failed&message="
                    + URLEncoder.encode(e.getMessage(), "UTF-8"));
        }
    }

    /**
     * POST /identity/auth/google/token
     * Alternative: Frontend exchanges Google authorization code directly (SPA flow).
     * Useful when frontend handles the OAuth redirect itself.
     */
    @PostMapping("auth/google/token")
    public ResponseEntity<ApiResponse<TokenResponse>> exchangeGoogleCode(
            @RequestBody Map<String, String> request,
            HttpServletResponse response
    ) {
        String code = request.get("code");
        String redirectUri = request.get("redirect_uri");

        TokenResponse tokenResponse = identityService.handleGoogleCallback(code, redirectUri);

        response.addCookie(cookieUtils.createCookie(
                tokenResponse.getRefreshToken(), CookieUtils.REFRESH_TOKEN_COOKIE));

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // =====================================================
    // Facebook OAuth2 Login
    // =====================================================

    /**
     * GET /identity/auth/facebook
     * Redirects user to Keycloak's Facebook broker login page.
     */
    @GetMapping("auth/facebook")
    public void redirectToFacebook(
            HttpServletResponse response,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri
    ) throws IOException {
        String facebookLoginUrl = identityService.buildFacebookLoginUrl(redirectUri);
        response.sendRedirect(facebookLoginUrl);
    }

    /**
     * GET /identity/auth/facebook/callback
     * Keycloak redirects here after Facebook authentication.
     * Exchanges authorization code for tokens, syncs profile, sets cookie, redirects to frontend.
     */
    @GetMapping("auth/facebook/callback")
    public void handleFacebookCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response
    ) throws IOException {
        try {
            TokenResponse tokenResponse = identityService.handleFacebookCallback(code);

            Cookie cookie = cookieUtils.createCookie(
                    tokenResponse.getRefreshToken(), CookieUtils.REFRESH_TOKEN_COOKIE);
            response.addCookie(cookie);

            String frontendUrl = identityService.getFacebookSuccessRedirectUrl();
            response.sendRedirect(frontendUrl
                    + "?access_token=" + tokenResponse.getAccessToken()
                    + "&expires_in=" + tokenResponse.getExpiresIn());

        } catch (Exception e) {
            String errorUrl = identityService.getFacebookErrorRedirectUrl();
            response.sendRedirect(errorUrl + "?error=facebook_login_failed&message="
                    + URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Unknown error", "UTF-8"));
        }
    }

    /**
     * POST /identity/auth/facebook/token
     * Alternative: Frontend exchanges Facebook authorization code directly (SPA flow).
     */
    @PostMapping("auth/facebook/token")
    public ResponseEntity<ApiResponse<TokenResponse>> exchangeFacebookCode(
            @RequestBody Map<String, String> request,
            HttpServletResponse response
    ) {
        String code = request.get("code");
        String redirectUri = request.get("redirect_uri");

        TokenResponse tokenResponse = identityService.handleFacebookCallback(code, redirectUri);

        response.addCookie(cookieUtils.createCookie(
                tokenResponse.getRefreshToken(), CookieUtils.REFRESH_TOKEN_COOKIE));

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

}
