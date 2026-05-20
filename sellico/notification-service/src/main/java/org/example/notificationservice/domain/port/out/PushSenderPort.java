package org.example.notificationservice.domain.port.out;

import lombok.Builder;
import lombok.Getter;

/**
 * Port for sending push notifications via WebSocket or Firebase
 */
public interface PushSenderPort {

    /**
     * Send a push notification
     * @param request the push request
     * @return SendPushResult with success/failure status
     */
    SendPushResult sendPush(SendPushRequest request);

    /**
     * Get the provider name (e.g., "WEBSOCKET", "FCM")
     */
    String getProviderName();

    @Getter
    @Builder
    class SendPushRequest {
        private String userId;
        private String title;
        private String body;
        private Object payload;
        private String type;
    }

    @Getter
    @Builder
    class SendPushResult {
        private boolean success;
        private String messageId;
        private String errorCode;
        private String errorMessage;

        public static SendPushResult success(String messageId) {
            return SendPushResult.builder()
                    .success(true)
                    .messageId(messageId)
                    .build();
        }

        public static SendPushResult failure(String errorCode, String errorMessage) {
            return SendPushResult.builder()
                    .success(false)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
