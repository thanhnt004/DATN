package org.example.identityservice.exception;

import response.BaseErrorCode;

public enum AuthErrorCode implements BaseErrorCode {
    USER_NAME_EXISTS("USER_NAME_EXISTS", "User name already exists, please choose another one", 400),
    EMAIL_EXISTS("EMAIL_EXISTS", "Email already exists, please choose another one", 400),
    PHONE_EXISTS("PHONE_EXISTS", "Phone number already exists, please choose another one", 400),
    USER_NAME_MISSING("USER_NAME_MISSING", "User name is required", 400),
    EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", "Email address is not verified", 400),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username or password", 401),
    INVALID_RESET_TOKEN("INVALID_RESET_TOKEN", "Invalid or expired password reset token", 400),
    RESET_TOKEN_EXPIRED("RESET_TOKEN_EXPIRED", "Password reset token has expired", 400),
    PASSWORDS_NOT_MATCH("PASSWORDS_NOT_MATCH", "New password and confirm password do not match", 400),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", 404),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found", 404),
    ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", "Role already exists", 409),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is locked! . Please try again later.", 403),
    INCORRECT_OLD_PASSWORD("INCORRECT_OLD_PASSWORD", "Current password is incorrect", 400),
    SAME_PASSWORD("SAME_PASSWORD", "New password must be different from current password", 400),
    GOOGLE_AUTH_FAILED("GOOGLE_AUTH_FAILED", "Google authentication failed", 401),
    GOOGLE_TOKEN_EXCHANGE_FAILED("GOOGLE_TOKEN_EXCHANGE_FAILED", "Failed to exchange Google authorization code for tokens", 500),
    FACEBOOK_AUTH_FAILED("FACEBOOK_AUTH_FAILED", "Facebook authentication failed", 401),
    FACEBOOK_TOKEN_EXCHANGE_FAILED("FACEBOOK_TOKEN_EXCHANGE_FAILED", "Failed to exchange Facebook authorization code for tokens", 500),
    EMAIL_ALREADY_VERIFIED("EMAIL_ALREADY_VERIFIED", "Email is already verified", 400),
    VERIFICATION_EMAIL_SEND_FAILED("VERIFICATION_EMAIL_SEND_FAILED", "Failed to send verification email", 500),
    ;

    AuthErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String code;
    private final String message;
    private final int statusCode;

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }
}
