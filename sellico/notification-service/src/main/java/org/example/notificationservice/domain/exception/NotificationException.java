package org.example.notificationservice.domain.exception;

import exception.BaseException;

public class NotificationException extends BaseException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }

    public static NotificationException notFound() {
        return new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotificationException templateNotFound() {
        return new NotificationException(NotificationErrorCode.TEMPLATE_NOT_FOUND);
    }

    public static NotificationException providerNotFound() {
        return new NotificationException(NotificationErrorCode.PROVIDER_NOT_FOUND);
    }

    public static NotificationException sendFailed() {
        return new NotificationException(NotificationErrorCode.SEND_FAILED);
    }

    public static NotificationException invalidRecipient() {
        return new NotificationException(NotificationErrorCode.INVALID_RECIPIENT);
    }

    public static NotificationException templateRenderError() {
        return new NotificationException(NotificationErrorCode.TEMPLATE_RENDER_ERROR);
    }
}

