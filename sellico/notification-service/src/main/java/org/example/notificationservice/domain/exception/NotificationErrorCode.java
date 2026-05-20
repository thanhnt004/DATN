package org.example.notificationservice.domain.exception;

import response.BaseErrorCode;

public enum NotificationErrorCode implements BaseErrorCode {
    NOTIFICATION_NOT_FOUND("NOTIF_001", "Notification not found", 404),
    TEMPLATE_NOT_FOUND("NOTIF_002", "Notification template not found", 404),
    PROVIDER_NOT_FOUND("NOTIF_003", "No active provider found for channel", 500),
    SEND_FAILED("NOTIF_004", "Failed to send notification", 500),
    INVALID_RECIPIENT("NOTIF_005", "Invalid recipient address", 400),
    TEMPLATE_RENDER_ERROR("NOTIF_006", "Failed to render template", 500),
    CHANNEL_LOG_NOT_FOUND("NOTIF_007", "Channel log not found", 404),
    MAX_RETRY_EXCEEDED("NOTIF_008", "Maximum retry attempts exceeded", 400);

    private final String code;
    private final String message;
    private final int statusCode;

    NotificationErrorCode(String code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

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

