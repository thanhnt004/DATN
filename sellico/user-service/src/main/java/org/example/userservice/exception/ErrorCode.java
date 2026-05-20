package org.example.userservice.exception;

import response.BaseErrorCode;

public enum ErrorCode implements BaseErrorCode {
    USER_NOT_FOUND("USER_001", "User not found", 404),
    PHONE_ALREADY_EXISTS("USER_002", "Phone number already exists", 409),
    EMAIL_ALREADY_EXISTS("USER_003", "Email already exists", 409),
    ADDRESS_NOT_FOUND("USER_004", "Address not found", 404),
    MAX_ADDRESSES_REACHED("USER_005", "Maximum number of addresses reached (10)", 400),
    CANNOT_DELETE_DEFAULT_ADDRESS("USER_006", "Cannot delete default address when it's the only one", 400),
    INVALID_GENDER("USER_007", "Invalid gender value", 400),
    UNAUTHORIZED("USER_008", "Unauthorized access", 401),
    ACCOUNT_ALREADY_DEACTIVATED("USER_009", "Account is already deactivated", 400),
    ACCOUNT_BANNED("USER_010", "Account has been banned", 403),
    SAME_EMAIL("USER_011", "New email is the same as current email", 400),
    INVALID_STATUS_TRANSITION("USER_012", "Invalid status transition", 400),
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", "User profile already exists", 409),
    USERNAME_ALREADY_EXISTS("USER_013", "Username already exists", 409),
    ;

    ErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final String code;
    private final String message;
    private final int statusCode;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
